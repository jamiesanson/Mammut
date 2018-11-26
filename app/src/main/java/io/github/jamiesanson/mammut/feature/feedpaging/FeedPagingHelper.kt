package io.github.jamiesanson.mammut.feature.feedpaging

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.Handler
import com.sys1yagi.mastodon4j.api.Pageable
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.Shutdownable
import com.sys1yagi.mastodon4j.api.entity.Notification
import io.github.jamiesanson.mammut.data.converters.toEntity
import io.github.jamiesanson.mammut.data.database.StatusDatabase
import io.github.jamiesanson.mammut.data.database.dao.StatusDao
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.extension.run
import io.github.jamiesanson.mammut.feature.base.Event
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedType
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.StreamingBuilder
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Take two: [FeedPagingHelper] is an attempt to create a more reliable and efficient paging handler
 * with support for streaming, proper ordering of statuses etc.
 */
class FeedPagingHelper(
        private val getCallForRange: (Range) -> MastodonRequest<Pageable<com.sys1yagi.mastodon4j.api.entity.Status>>,
        val getPreviousPosition: () -> Int?,
        private val setPreviousPosition: (Int) -> Unit,
        private val streamingBuilder: StreamingBuilder?,
        private val statusDatabase: StatusDatabase,
        private val feedType: FeedType
) : PagedList.BoundaryCallback<Status>(), CoroutineScope, Handler {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO // TODO - Add error handler specific to the helper

    private val statusDao: StatusDao = statusDatabase.statusDao()

    private var streamShutdownable: Shutdownable? = null
    private var bufferedItemDisposable: Disposable? = null

    private val dbPagingExecutor: Executor = Executors.newSingleThreadExecutor()

    private val pagingHelper: PagingRequestHelper = PagingRequestHelper(dbPagingExecutor)

    private val networkState = pagingHelper.createStatusLiveData()

    private val itemStreamedPublishSubject = PublishSubject.create<com.sys1yagi.mastodon4j.api.entity.Status>()
    private val bufferedItemFlowable: Flowable<Event<List<com.sys1yagi.mastodon4j.api.entity.Status>>> = Flowable
                .fromPublisher(itemStreamedPublishSubject.toFlowable(BackpressureStrategy.BUFFER))
                .buffer(100, TimeUnit.MILLISECONDS)
                .map { Event(it) }
    private val itemStreamed: LiveData<Event<List<com.sys1yagi.mastodon4j.api.entity.Status>>> = LiveDataReactiveStreams
            .fromPublisher(bufferedItemFlowable)

    private val timelineBroken: LiveData<Boolean> = MutableLiveData()

    private val isTimelineBroken: Deferred<Boolean> = async {
        checkIfIsTimelineBroken().also {
            timelineBroken.postSafely(it)
        }
    }

    fun initialise(): FeedData<Status> {
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh()
        }

        val liveList = LivePagedListBuilder(statusDao.getAllPagedInFeed(feedType.key), 50)
                .setBoundaryCallback(this)
                .setInitialLoadKey(getPreviousPosition())
                .build()

        launch {
            // Only begin streaming if the timeline isn't broken
            if (!isTimelineBroken.await()) {
                beginStreaming()
            }
        }

        return FeedData(
                pagedList = liveList,
                networkState = networkState,
                itemStreamed = itemStreamed,
                retry = {
                    pagingHelper.retryAllFailed()
                },
                refresh = {
                    refreshTrigger.value = null
                },
                refreshState = refreshState
        )
    }

    fun onCleared(lastPageSeen: Int?) {
        lastPageSeen?.let(setPreviousPosition)
        stopStreaming()
    }

    private suspend fun beginStreaming() = coroutineScope {
        streamShutdownable = streamingBuilder?.startStream(this@FeedPagingHelper)

        // Begin listening to buffered results
        bufferedItemDisposable = bufferedItemFlowable
                .subscribeOn(Schedulers.from(dbPagingExecutor))
                .subscribe {
                    launch {
                        insertStatuses(it.peekContent(), true)
                    }
                }
    }

    private fun stopStreaming() {
        launch {
            streamShutdownable?.shutdown()
            streamShutdownable = null
            bufferedItemDisposable?.dispose()
        }
    }

    /**
     * This is to be called each time the [FeedPagingHelper] is initialised, if operating
     * on a feed where persistence is enabled.
     *
     * How this works:
     * * Load the most N recent statuses in the feed
     * * Load the top most N statuses in the database
     * * Calculate the average time between posts for both lists
     * * If difference between oldest from the remote, and newest locally is larger than X times that
     *   the timeline is broken, so give the user a chance to choose if they want to continue on up or
     *   jump to the top.
     */
    private suspend fun checkIfIsTimelineBroken(): Boolean = coroutineScope {
        val N = 20
        val X = 3

        val result = getCallForRange(Range(limit = N)).run(retryCount = 3)

        // Ignore errors here - if no results, just say the timeline's broken and see what happens.
        val remotePage = result.toOption().orNull()?.part?.map { it.toEntity() } ?: return@coroutineScope true
        val localPage = statusDao.getMostRecent(count = N, source = feedType.key)

        // Average the time between items
        fun List<Status>.getAverageInterval(): Duration =
            windowed(size = 2, step = 1) { items ->
                val (first, second) = items.map { ZonedDateTime.parse(it.createdAt) }
                Duration.between(first, second)
            }.reduce { acc, duration ->
                acc + duration
            }.dividedBy(size.toLong())

        val remoteInterval = remotePage.getAverageInterval()
        val localInterval = localPage.getAverageInterval()
        val totalInterval = (remoteInterval + localInterval).dividedBy(2L)

        val oldestRemoteTime = ZonedDateTime.parse(remotePage.last().createdAt)
        val latestLocalTime = ZonedDateTime.parse(localPage.first().createdAt)

        val middleInterval = Duration.between(oldestRemoteTime, latestLocalTime)

        middleInterval.multipliedBy(X.toLong()) > totalInterval
    }

    private fun executeStatusRequest(request: MastodonRequest<Pageable<com.sys1yagi.mastodon4j.api.entity.Status>>, callback: PagingRequestHelper.Request.Callback, isLoadingInfront: Boolean = false) {
        launch {
            val result = request.run(retryCount = 3)

            when (result) {
                is Either.Left -> {
                    callback.recordFailure(Exception(result.a.description))
                }
                is Either.Right -> {
                    insertStatuses(result.b.part, isLoadingInfront)

                    callback.recordSuccess()
                }
            }
        }
    }

    private suspend fun insertStatuses(statuses: List<com.sys1yagi.mastodon4j.api.entity.Status>, addToFront: Boolean) = coroutineScope {
        statusDatabase.runInTransaction {
            when {
                addToFront -> {
                    val frontIndex = statusDao.getPreviousIndexInFeed(feedType.key)
                    statusDao.insertNewPage(statuses.mapIndexed { index, status ->
                        status.toEntity().copy(
                                statusIndex = frontIndex + (index - statuses.size),
                                source = feedType.key
                        )
                    })
                }
                else -> {
                    val endIndex = statusDao.getNextIndexInFeed(feedType.key)
                    statusDao.insertNewPage(statuses.mapIndexed { index, status ->
                        status.toEntity().copy(
                                statusIndex = index + endIndex,
                                source = feedType.key
                        )
                    })
                }
            }
        }
    }

    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.Running

        launch {
            val result = getCallForRange(Range()).run(retryCount = 3)
            when (result) {
                is Either.Left -> {
                    networkState.value = NetworkState.Error(result.a.description)
                }
                is Either.Right -> {
                    statusDatabase.runInTransaction {
                        statusDao.deleteByFeed(feedType.key)
                        val endIndex = 0
                        statusDao.insertNewPage(result.b.part.mapIndexed { index, status ->
                            status.toEntity().copy(
                                    statusIndex = index + endIndex,
                                    source = feedType.key
                            )
                        })
                    }

                    networkState.postValue(NetworkState.Loaded)
                }
            }
        }

        return networkState
    }

    // REGION BoundaryCallback
    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        pagingHelper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            executeStatusRequest(getCallForRange(Range()), it)
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Status) {
        super.onItemAtFrontLoaded(itemAtFront)
        // If timeline broken, or we're streaming, we shouldn't go ahead with this.
        launch {
            if (isTimelineBroken.await() || streamShutdownable != null) {
                return@launch
            }

            withContext(Dispatchers.Main) {
                pagingHelper.runIfNotRunning(PagingRequestHelper.RequestType.BEFORE) {
                    executeStatusRequest(getCallForRange(Range(minId = itemAtFront.id)), it, isLoadingInfront = true)
                }
            }
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: Status) {
        super.onItemAtEndLoaded(itemAtEnd)
        pagingHelper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            executeStatusRequest(getCallForRange(Range(maxId = itemAtEnd.id)), it)
        }
    }
    // END REGION

    // REGION Handler
    override fun onDelete(id: Long) {
        launch {
            statusDao.deleteById(id)
        }
    }

    override fun onNotification(notification: Notification) {
        // no-op
    }

    override fun onStatus(status: com.sys1yagi.mastodon4j.api.entity.Status) {
        itemStreamedPublishSubject.onNext(status)
    }
    // END REGION
}
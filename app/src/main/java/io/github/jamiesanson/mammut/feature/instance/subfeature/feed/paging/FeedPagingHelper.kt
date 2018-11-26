package io.github.jamiesanson.mammut.feature.instance.subfeature.feed.paging

import androidx.annotation.MainThread
import androidx.lifecycle.*
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
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

    private val dbPagingExecutor: Executor = Executors.newSingleThreadExecutor()

    private val pagingHelper: PagingRequestHelper = PagingRequestHelper(dbPagingExecutor)

    private val networkState = pagingHelper.createStatusLiveData()

    private val itemStreamed: LiveData<Event<Unit?>> = MutableLiveData()

    fun initialise(): FeedData<Status> {
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh()
        }

        val liveList = LivePagedListBuilder(statusDao.getAllPagedInFeed(feedType.key), 50)
                .setBoundaryCallback(this)
                .setInitialLoadKey(getPreviousPosition())
                .build()

        beginStreaming()

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

    private fun beginStreaming() {
        launch {
            streamShutdownable = streamingBuilder?.startStream(this@FeedPagingHelper)
        }
    }

    private fun stopStreaming() {
        streamShutdownable?.shutdown()
        streamShutdownable = null
    }

    private fun executeStatusRequest(request: MastodonRequest<Pageable<com.sys1yagi.mastodon4j.api.entity.Status>>, callback: PagingRequestHelper.Request.Callback, isLoadingInfront: Boolean = false) {
        launch {
            val result = request.run(retryCount = 3)

            when (result) {
                is Either.Left -> {
                    callback.recordFailure(Exception(result.a.description))
                }
                is Either.Right -> {
                    statusDatabase.runInTransaction {
                        when {
                            isLoadingInfront -> {
                                val frontIndex = statusDao.getPreviousIndexInFeed(feedType.key)
                                statusDao.insertNewPage(result.b.part.mapIndexed { index, status ->
                                    status.toEntity().copy(
                                            statusIndex = frontIndex + (index - result.b.part.size),
                                            source = feedType.key
                                    )
                                })
                            }
                            else -> {
                                val endIndex = statusDao.getNextIndexInFeed(feedType.key)
                                statusDao.insertNewPage(result.b.part.mapIndexed { index, status ->
                                    status.toEntity().copy(
                                            statusIndex = index + endIndex,
                                            source = feedType.key
                                    )
                                })
                            }
                        }
                    }
                    callback.recordSuccess()
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
        pagingHelper.runIfNotRunning(PagingRequestHelper.RequestType.BEFORE) {
            executeStatusRequest(getCallForRange(Range(minId = itemAtFront.id)), it, isLoadingInfront = true)
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
        launch {
            statusDatabase.runInTransaction {
                val index = statusDao.getNextIndexInFeed(feedType.key)
                statusDao.insertStatus(status.toEntity().apply {
                    copy(
                            statusIndex = index,
                            source = this@FeedPagingHelper.feedType.key
                    )
                })
            }

            itemStreamed.postSafely(Event(null))
        }
    }
    // END REGION
}
package io.github.koss.mammut.feature.feedpaging

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.Pageable
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.data.converters.toLocalModel
import io.github.koss.mammut.data.database.StatusDatabase
import io.github.koss.mammut.data.database.dao.StatusDao
import io.github.koss.mammut.data.extensions.run
import io.github.koss.mammut.feature.instance.subfeature.feed.FeedType
import io.github.koss.mammut.feature.instance.subfeature.feed.dagger.StreamingBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class FeedPager(
        private val getCallForRange: (Range) -> MastodonRequest<Pageable<Status>>,
        val getPreviousPosition: () -> Int?,
        private val setPreviousPosition: (Int) -> Unit,
        streamingBuilder: StreamingBuilder?,
        private val statusDatabase: StatusDatabase,
        private val feedType: FeedType,
        private val feedStateData: FeedStateData
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO // TODO - Add error handler specific to the helper

    private val statusDao: StatusDao = statusDatabase.statusDao()

    private val dbExecutor = Executors.newSingleThreadExecutor()

    private val streamHandler = FeedStreamingHandler(
            feedState = feedStateData.observableState,
            streamingBuilder = streamingBuilder,
            handleStatuses = ::insertStatuses,
            onItemDeleted = {
                statusDao.deleteById(it)
            })

    private val boundaryCallback = FeedPagingBoundaryCallback(
            dbExecutor,
            getCallForRange,
            ::insertStatuses,
            feedStateData.observableState
    )

    private fun refreshState() {
        feedStateData.store.send(if (feedType.supportsStreaming) FeedStateEvent.OnFreshFeed(feedType.supportsStreaming) else FeedStateEvent.OnBrokenTimelineResolved)
    }

    /**
     * Initialise and begin paging
     */
    fun initialise(): FeedData<io.github.koss.mammut.data.database.entities.feed.Status> {
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh()
        }

        val liveList = LivePagedListBuilder(statusDao.getAllPagedInFeed(feedType.key), 20)
                .setBoundaryCallback(boundaryCallback)
                .setInitialLoadKey(getPreviousPosition())
                .build()

        return FeedData<io.github.koss.mammut.data.database.entities.feed.Status>(
                pagedList = liveList,
                networkState = boundaryCallback.networkState,
                itemStreamed = streamHandler.itemStreamed,
                retry = {
                    boundaryCallback.helper.retryAllFailed()
                },
                refresh = {
                    refreshTrigger.value = null
                },
                refreshState = refreshState,
                state = feedStateData.observableState
        )
    }

    fun forceLoadAtFront(frontItem: io.github.koss.mammut.data.database.entities.feed.Status) {
        boundaryCallback.onItemAtFrontLoaded(frontItem)
    }

    fun onCleared(lastPageSeen: Int?) {
        lastPageSeen?.let(setPreviousPosition)
    }

    fun tearDown() {
        streamHandler.tearDown()
    }

    /**
     * Function for handling a set of results returned by the API.
     */
    private suspend fun insertStatuses(statuses: List<Status>) = coroutineScope {
        statusDao.insertNewPage(statuses.map { status ->
            status.toLocalModel().copy(
                    source = feedType.key
            )
        })
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
        refreshState()

        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.Running(initial = true)

        launch {
            val result = getCallForRange(Range()).run(retryCount = 3)
            when (result) {
                is Either.Left -> {
                    networkState.postValue(NetworkState.Error(result.a.description))
                }
                is Either.Right -> {
                    statusDatabase.runInTransaction {
                        statusDao.deleteByFeed(feedType.key)
                        val endIndex = 0
                        statusDao.insertNewPage(result.b.part.mapIndexed { index, status ->
                            status.toLocalModel().copy(
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
}


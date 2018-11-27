package io.github.jamiesanson.mammut.feature.feedpaging

import android.util.Log
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
import io.github.jamiesanson.mammut.data.converters.toEntity
import io.github.jamiesanson.mammut.data.database.StatusDatabase
import io.github.jamiesanson.mammut.data.database.dao.StatusDao
import io.github.jamiesanson.mammut.data.repo.PreferencesRepository
import io.github.jamiesanson.mammut.extension.run
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedType
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.StreamingBuilder
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

    private fun refreshState() {
        feedStateData.store.send(if (feedType.supportsStreaming) FeedStateEvent.OnFreshFeed else FeedStateEvent.OnBrokenTimelineResolved)
    }

    private fun onBrokenTimelineResolved() {

    }

    /**
     * Initialise and begin paging
     */
    fun initialise(): FeedData<io.github.jamiesanson.mammut.data.database.entities.feed.Status> {
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh()
        }

        val boundaryCallback = FeedPagingBoundaryCallback(
                dbExecutor,
                getCallForRange,
                ::insertStatuses,
                feedStateData.observableState
        )

        val liveList = LivePagedListBuilder(statusDao.getAllPagedInFeed(feedType.key), 20)
                .setBoundaryCallback(boundaryCallback)
                .setInitialLoadKey(getPreviousPosition())
                .build()

        return FeedData<io.github.jamiesanson.mammut.data.database.entities.feed.Status>(
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

    fun onCleared(lastPageSeen: Int?) {
        lastPageSeen?.let(setPreviousPosition)
        streamHandler.tearDown()
    }

    /**
     * Function for handling a set of results returned by the API.
     */
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
        Log.d("Pager", "Refreshing from state ${feedStateData.store.state}")
        refreshState()

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
}


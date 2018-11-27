package io.github.jamiesanson.mammut.feature.feedpaging

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.feature.feedpaging.scaffold.Reducible
import io.github.jamiesanson.mammut.feature.feedpaging.scaffold.StateObserver
import io.github.jamiesanson.mammut.feature.feedpaging.scaffold.Store
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime

sealed class FeedState: Reducible<FeedStateEvent, FeedState> {

    override fun reduce(event: FeedStateEvent): FeedState =
            when {
                this is Undefined && event is FeedStateEvent.OnTimelineBroken ->
                    BrokenTimeline
                this is Undefined && event is FeedStateEvent.OnFreshFeed ->
                    StreamingFromTop
                this is Undefined && event is FeedStateEvent.OnBrokenTimelineResolved ->
                    PagingUpwards

                this is BrokenTimeline && event is FeedStateEvent.OnBrokenTimelineResolved ->
                    PagingUpwards
                this is BrokenTimeline && event is FeedStateEvent.OnFreshFeed ->
                    StreamingFromTop

                this is PagingUpwards && event is FeedStateEvent.OnFreshFeed -> {
                    StreamingFromTop
                }
                else -> this
            }


    object StreamingFromTop: FeedState()

    object BrokenTimeline: FeedState()

    object PagingUpwards: FeedState()

    object Undefined: FeedState()
}

sealed class FeedStateEvent {

    object OnTimelineBroken: FeedStateEvent()

    object OnFreshFeed: FeedStateEvent()

    object OnBrokenTimelineResolved: FeedStateEvent()
}


data class FeedStateData(
        val store: Store<FeedState, FeedStateEvent>,
        val observableState: LiveData<FeedState>
)

fun initialiseFeedState(
        streamingEnabled: Boolean,
        keepPlaceEnabled: Boolean,
        scrolledToTop: Boolean,
        loadRemotePage: suspend () -> List<Status>?,
        loadLocalPage: suspend () -> List<Status>): FeedStateData {

    val store = Store(initialState = FeedState.Undefined)
    val stateLiveData: LiveData<FeedState> = MutableLiveData()

    store.observe(object : StateObserver<FeedState> {
        override fun stateChanged(oldState: FeedState?, newState: FeedState) {
            Log.d("FeedStateMachine", "Feed transitioning from $oldState to $newState")
            stateLiveData.postSafely(newState)
        }
    })

    // If this is a fresh instance, we should return a fresh state
    if (!keepPlaceEnabled) {
        store.send(if (streamingEnabled) FeedStateEvent.OnFreshFeed else FeedStateEvent.OnBrokenTimelineResolved)
        return FeedStateData(
                store,
                stateLiveData
        )
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
    GlobalScope.launch {
        val X = 3

        // Ignore errors here - if no results, just say the timeline's broken and see what happens.
        val remotePage = loadRemotePage() ?: run {
            store.send(FeedStateEvent.OnTimelineBroken)
            return@launch
        }
        val localPage = loadLocalPage()

        if (localPage.isEmpty()) {
            store.send(if (streamingEnabled) FeedStateEvent.OnFreshFeed else FeedStateEvent.OnBrokenTimelineResolved)
            return@launch
        }

        // Average the time between items
        fun List<Status>.getAverageInterval(): Duration =
                windowed(size = 2, step = 1) { items ->
                    val (first, second) = items.map { ZonedDateTime.parse(it.createdAt) }
                    Duration.between(first, second)
                }.run {
                    if (isNotEmpty()) {
                        reduce { acc, duration ->
                            acc + duration
                        }.dividedBy(size.toLong())
                    } else {
                        // We just need a long duration here to ensure results are coherent
                        Duration.ofDays(14L)
                    }
                }

        val remoteInterval = remotePage.getAverageInterval()
        val localInterval = localPage.getAverageInterval()
        val totalInterval = (remoteInterval + localInterval).dividedBy(2L)

        val oldestRemoteTime = ZonedDateTime.parse(remotePage.last().createdAt)
        val latestLocalTime = ZonedDateTime.parse(localPage.first().createdAt)

        if (oldestRemoteTime.isBefore(latestLocalTime)) {
            store.send(FeedStateEvent.OnBrokenTimelineResolved)
            return@launch
        }

        val middleInterval = Duration.between(oldestRemoteTime, latestLocalTime)

        val isBroken = middleInterval > totalInterval.multipliedBy(X.toLong())
        if (isBroken) {
            store.send(FeedStateEvent.OnTimelineBroken)
        } else {
            if (scrolledToTop) {
                store.send(FeedStateEvent.OnFreshFeed)
            } else {
                store.send(FeedStateEvent.OnBrokenTimelineResolved)
            }
        }
    }

    return FeedStateData(
            store,
            stateLiveData
    )
}
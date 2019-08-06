package io.github.koss.mammut.feature.feedpaging

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.koss.mammut.base.util.postSafely
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.feature.feedpaging.scaffold.Reducible
import io.github.koss.mammut.feature.feedpaging.scaffold.StateObserver
import io.github.koss.mammut.feature.feedpaging.scaffold.Store
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import kotlin.math.abs

sealed class FeedState: Reducible<FeedStateEvent, FeedState> {

    override fun reduce(event: FeedStateEvent): FeedState =
            when {
                this is Undefined && event is FeedStateEvent.OnTimelineBroken ->
                    BrokenTimeline
                this is Undefined && event is FeedStateEvent.OnFreshFeed ->
                    if (event.streamingEnabled) StreamingFromTop else PagingUpwards
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

    data class OnFreshFeed(val streamingEnabled: Boolean): FeedStateEvent()

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
        store.send(if (streamingEnabled) FeedStateEvent.OnFreshFeed(streamingEnabled) else FeedStateEvent.OnBrokenTimelineResolved)
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
        val X = 20

        val remotePage = loadRemotePage() ?: run {
            store.send(FeedStateEvent.OnBrokenTimelineResolved)
            return@launch
        }
        val localPage = loadLocalPage()

        if (localPage.isEmpty()) {
            store.send(if (streamingEnabled) FeedStateEvent.OnFreshFeed(streamingEnabled) else FeedStateEvent.OnBrokenTimelineResolved)
            return@launch
        }

        if (remotePage.first().id == localPage.first().id) {
            // Begin streaming if applicable
            if (streamingEnabled) {
                store.send(FeedStateEvent.OnFreshFeed(streamingEnabled))
                return@launch
            }
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

        val isBroken = abs(middleInterval.toMillis()) > abs(totalInterval.multipliedBy(X.toLong()).toMillis())
        if (isBroken) {
            store.send(FeedStateEvent.OnTimelineBroken)
        } else {
            if (scrolledToTop) {
                store.send(FeedStateEvent.OnFreshFeed(streamingEnabled))
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
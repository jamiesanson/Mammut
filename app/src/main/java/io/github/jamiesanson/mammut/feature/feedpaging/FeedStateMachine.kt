package io.github.jamiesanson.mammut.feature.feedpaging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.extension.postSafely
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import kotlin.properties.Delegates

object FeedStateMachine {

    private var state: FeedState by Delegates.observable(initialValue = FeedState.Undefined) { _, _: FeedState, new: FeedState ->
        if (new != FeedState.Undefined) {
            stateLiveData.postSafely(new)
        }
    }

    val stateLiveData: LiveData<FeedState> = MutableLiveData()

    fun onBeginPagingUpwards() {
        state = FeedState.PagingUpwards
    }

    fun onNewFeedStarted() {
        state = FeedState.StreamingFromTop
    }

    fun onBrokenTimeline() {
        state = FeedState.BrokenTimeline
    }
}

fun initialiseFeedState(
        keepPlaceEnabled: Boolean,
        scrolledToTop: Boolean,
        loadRemotePage: suspend () -> List<Status>?,
        loadLocalPage: suspend () -> List<Status>) {

    if (!keepPlaceEnabled || scrolledToTop) {
        FeedStateMachine.onNewFeedStarted()
        return
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
            FeedStateMachine.onBrokenTimeline()
            return@launch
        }
        val localPage = loadLocalPage()

        if (localPage.isEmpty()) {
            FeedStateMachine.onNewFeedStarted()
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

        val middleInterval = Duration.between(oldestRemoteTime, latestLocalTime)

        val isBroken = middleInterval.multipliedBy(X.toLong()) > totalInterval
        if (isBroken) {
            FeedStateMachine.onBrokenTimeline()
        } else {
            FeedStateMachine.onBeginPagingUpwards()
        }

    }
}
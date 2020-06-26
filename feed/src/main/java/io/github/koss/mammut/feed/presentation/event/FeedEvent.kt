package io.github.koss.mammut.feed.presentation.event

/**
 * Events sent from ViewModel -> View for display
 */
sealed class FeedEvent

object ItemStreamed: FeedEvent()

sealed class Navigation: FeedEvent() {

    data class Profile(val userId: String): Navigation()

    data class Tag(val tagName: String): Navigation()
}
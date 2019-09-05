package io.github.koss.mammut.feed.presentation.event

/**
 * Events sent from ViewModel -> View for display
 */
sealed class FeedEvent

object ItemStreamed: FeedEvent()
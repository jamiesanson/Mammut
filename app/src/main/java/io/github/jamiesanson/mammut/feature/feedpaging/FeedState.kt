package io.github.jamiesanson.mammut.feature.feedpaging

sealed class FeedState {

    object StreamingFromTop: FeedState()

    object BrokenTimeline: FeedState()

    object PagingUpwards: FeedState()

    object Undefined: FeedState()
}
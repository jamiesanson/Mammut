package io.github.koss.mammut.feed.presentation.state

import io.github.koss.mammut.feed.presentation.model.FeedModel

sealed class FeedState

object LoadingAll: FeedState()

data class Loaded(
    val loadingAtFront: Boolean,
    val loadingAtEnd: Boolean,
    val items: List<FeedModel>
): FeedState()


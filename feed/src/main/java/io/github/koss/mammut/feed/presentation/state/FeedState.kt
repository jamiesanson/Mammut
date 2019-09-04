package io.github.koss.mammut.feed.presentation.state

import io.github.koss.mammut.feed.presentation.model.FeedModel

sealed class FeedState

object LoadingAll: FeedState()

data class Loaded(
    val loadingAtFront: Boolean = false,
    val loadingAtEnd: Boolean = false,
    val items: List<FeedModel>
): FeedState()


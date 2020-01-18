package io.github.koss.mammut.feed.presentation.state

import io.github.koss.mammut.feed.presentation.model.FeedModel

sealed class FeedState

data class LoadingAll(
    val initialPosition: Int = -1
): FeedState()

data class Loaded(
    val loadingAtFront: Boolean = false,
    val loadingAtEnd: Boolean = false,
    val initialPosition: Int = -1,
    val items: List<FeedModel>
): FeedState()


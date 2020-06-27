package io.github.koss.mammut.feed.presentation.state

import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.presentation.model.StatusModel
import io.github.koss.paging.network.LoadingState

sealed class FeedAction

object OnBrokenTimeline: FeedAction()

data class OnLoadingStateChanged(
    val loadingState: LoadingState
): FeedAction()

data class OnItemsLoaded(
    val items: List<Status>
): FeedAction()

data class OnItemsRendered(
    val items: List<Status>,
    val renderedItems: List<StatusModel>,
    val spansReplaced: Boolean = false
): FeedAction()
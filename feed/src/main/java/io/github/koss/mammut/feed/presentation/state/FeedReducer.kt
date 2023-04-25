package io.github.koss.mammut.feed.presentation.state

import io.github.koss.mammut.feed.presentation.model.BrokenTimelineModel
import io.github.koss.paging.network.LoadingAtEnd
import io.github.koss.paging.network.LoadingAtFront
import io.github.koss.randux.utils.Action
import io.github.koss.randux.utils.Reducer
import io.github.koss.randux.utils.State

class FeedReducer : Reducer {

    override fun invoke(currentState: State?, incomingAction: Action): State? =
        (currentState as? FeedState)?.let { state ->
            when (state) {
                is LoadingAll -> {
                    when (incomingAction) {
                        is OnItemsRendered -> {
                            return@let Loaded(
                                items = incomingAction.renderedItems,
                                initialPosition = state.initialPosition
                            )
                        }
                    }
                }
                is Loaded -> {
                    when (incomingAction) {
                        OnBrokenTimeline -> {
                            return@let state.copy(
                                items = listOf(BrokenTimelineModel, *state.items.toTypedArray())
                            )
                        }

                        is OnLoadingStateChanged -> {
                            return@let when (incomingAction.loadingState) {
                                is io.github.koss.paging.network.LoadingAll -> LoadingAll()
                                else -> state.copy(
                                        loadingAtFront = incomingAction.loadingState is LoadingAtFront,
                                        loadingAtEnd = incomingAction.loadingState is LoadingAtEnd
                                )
                            }
                        }

                        is OnItemsRendered -> {
                            return@let state.copy(
                                items = incomingAction.renderedItems
                            )
                        }
                    }
                }
            }

            when (incomingAction) {
                is OnLoadingStateChanged -> {
                    when (state) {
                        is Loaded -> return@let state.copy(
                            loadingAtFront = incomingAction.loadingState is LoadingAtFront,
                            loadingAtEnd = incomingAction.loadingState is LoadingAtEnd)
                        else -> {}
                    }

                    when (incomingAction.loadingState) {
                        is io.github.koss.paging.network.LoadingAll -> return@let LoadingAll()
                        else -> {}
                    }
                }
            }

            return@let null
        } ?: currentState
}
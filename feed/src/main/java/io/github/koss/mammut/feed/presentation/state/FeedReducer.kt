package io.github.koss.mammut.feed.presentation.state

import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.presentation.model.BrokenTimelineModel
import io.github.koss.mammut.feed.presentation.model.StatusModel
import io.github.koss.paging.network.LoadingAtEnd
import io.github.koss.paging.network.LoadingAtFront
import io.github.koss.randux.utils.Action
import io.github.koss.randux.utils.Reducer
import io.github.koss.randux.utils.State

class FeedReducer : Reducer {

    override fun invoke(currentState: State?, incomingAction: Action): State? =
        (currentState as? FeedState)?.let { state ->
            when (state) {
                LoadingAll -> {
                    when (incomingAction) {
                        is OnItemsLoaded -> {
                            return@let Loaded(
                                items = incomingAction.items.map { it.toModel() }
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
                            return@let state.copy(
                                loadingAtFront = incomingAction.loadingState is LoadingAtFront,
                                loadingAtEnd = incomingAction.loadingState is LoadingAtEnd
                            )
                        }

                        is OnItemsLoaded -> {
                            return@let state.copy(
                                items = incomingAction.items.map { it.toModel() }
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
                    }

                    when (incomingAction.loadingState) {
                        is io.github.koss.paging.network.LoadingAll -> return@let LoadingAll
                    }
                }
            }

            return@let null
        } ?: currentState

    private fun Status.toModel(): StatusModel = StatusModel(this)
}
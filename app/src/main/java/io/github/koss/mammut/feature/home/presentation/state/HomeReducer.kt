package io.github.koss.mammut.feature.home.presentation.state

import io.github.koss.mammut.data.extensions.fullAcct
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.randux.utils.Action
import io.github.koss.randux.utils.Reducer
import io.github.koss.randux.utils.State

class HomeReducer : Reducer {

    override fun invoke(currentState: State?, incomingAction: Action): State? =
            (currentState as? HomeState)?.let { state ->
                when (incomingAction) {
                    is OnRegistrationsLoaded -> {
                        return@let state.copy(
                                currentUser = incomingAction
                                        .registrations
                                        .first { it.accessToken?.accessToken == state.accessToken }
                                        .account!!,
                                allAccounts = incomingAction.registrations
                                        .mapNotNull { it.account?.copy(acct = it.account!!.fullAcct(it.instanceName)) }
                                        .toSet()
                        )
                    }
                    is OnFeedTypeChanged -> {
                        when (incomingAction.newFeedType) {
                            FeedType.Home,
                            FeedType.Local,
                            FeedType.Federated -> { /* no-op, these are supported */
                            }
                            else -> throw IllegalArgumentException("Switching to feed type ${incomingAction.newFeedType} not supported")
                        }

                        return@let state.copy(
                                selectedFeedType = incomingAction.newFeedType
                        )
                    }
                    is OnOffscreenItemCountChanged -> {
                        return@let state.copy(
                                offscreenItemCount = incomingAction.newCount
                        )
                    }
                }

                return@let null
            } ?: currentState
}
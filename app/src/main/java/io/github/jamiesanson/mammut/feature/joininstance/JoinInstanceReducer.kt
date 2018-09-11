package io.github.jamiesanson.mammut.feature.joininstance

import io.github.koss.randux.utils.Action
import io.github.koss.randux.utils.Reducer
import io.github.koss.randux.utils.State

class JoinInstanceReducer : Reducer {
    override fun invoke(currentState: State?, incomingAction: Action): State? =
            (currentState as? JoinInstanceState)?.let { state ->
                when (incomingAction) {
                    is InstanceUrlChanged -> {
                        JoinInstanceState.instanceUrl.modify(state) {
                            incomingAction.url
                        }
                    }
                    else -> currentState
                }
            }

}
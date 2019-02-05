package io.github.koss.mammut.feature.joininstance

import io.github.koss.mammut.architecture.Model
import io.github.koss.mammut.architecture.SideEffect

/**
 * Model representing the flow of data through the Join Instance screen
 */
class JoinInstanceModel: Model<JoinInstanceViewState>(JoinInstanceViewState()) {

    override fun handleSideEffect(currentState: JoinInstanceViewState, sideEffect: SideEffect): JoinInstanceViewState {
        TODO("not implemented")
    }
}
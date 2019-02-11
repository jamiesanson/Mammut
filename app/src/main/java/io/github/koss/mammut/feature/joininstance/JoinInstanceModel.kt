package io.github.koss.mammut.feature.joininstance

import android.content.res.Resources
import io.github.koss.mammut.architecture.Model
import io.github.koss.mammut.architecture.SideEffect
import io.github.koss.mammut.architecture.common.Loading
import io.github.koss.mammut.architecture.common.ResolvableString
import io.github.koss.mammut.data.models.InstanceSearchResult
import io.github.koss.mammut.feature.joininstance.sideeffects.OAuthUrlFormed
import javax.inject.Inject

/**
 * Model representing the flow of data through the Join Instance screen
 */
class JoinInstanceModel @Inject constructor(

): Model<JoinInstanceViewState>(initialViewState = JoinInstanceViewState.INITIAL) {

    fun loadingStarted() {
        onNewSideEffect(Loading(true))
    }

    fun loadingFinished() {
        onNewSideEffect(Loading(false))
    }

    fun onError(resolver: (Resources) -> String) {
        onNewSideEffect(ResolvableString(resolver))
    }

    fun oauthUrlFormed(url: String) {
        onNewSideEffect(OAuthUrlFormed(url))
    }

    fun searchResultsRetrieved(searchResults: List<InstanceSearchResult>) {

    }

    override fun handleSideEffect(currentState: JoinInstanceViewState, sideEffect: SideEffect): JoinInstanceViewState {
        TODO("not implemented")
    }
}
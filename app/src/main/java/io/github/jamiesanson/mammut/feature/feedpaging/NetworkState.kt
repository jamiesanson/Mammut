package io.github.jamiesanson.mammut.feature.feedpaging

sealed class NetworkState {

    object Running: NetworkState()

    object Loaded: NetworkState()

    data class Error(val message: String): NetworkState()
}
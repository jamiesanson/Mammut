package io.github.jamiesanson.mammut.feature.feedpaging

sealed class NetworkState {

    data class Running(val start: Boolean = false, val end: Boolean = false, val initial: Boolean = false): NetworkState()

    object Loaded: NetworkState()

    data class Error(val message: String): NetworkState()
}
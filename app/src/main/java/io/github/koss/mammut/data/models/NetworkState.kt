package io.github.koss.mammut.data.models

sealed class NetworkState {

    object Loading: NetworkState()

    object Loaded: NetworkState()

    object Offline: NetworkState()
}
package io.github.koss.paging.network

sealed class LoadConfig<NetworkModel>

class Initial<NetworkModel>: LoadConfig<NetworkModel>()

data class Before<NetworkModel>(val item: NetworkModel): LoadConfig<NetworkModel>()

data class After<NetworkModel>(val item: NetworkModel): LoadConfig<NetworkModel>()
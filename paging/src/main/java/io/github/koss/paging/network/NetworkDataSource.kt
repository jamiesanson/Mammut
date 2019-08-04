package io.github.koss.paging.network

interface NetworkDataSource<NetworkModel> {

    suspend fun loadMore(config: LoadConfig<NetworkModel>): List<NetworkModel>
}
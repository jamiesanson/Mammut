package io.github.koss.paging.network

import kotlinx.coroutines.flow.Flow

interface StreamingSupportedDataSource<NetworkModel> {

    val streamedResults: Flow<NetworkModel>

    suspend fun activate() {}

    suspend fun deactivate() {}
}
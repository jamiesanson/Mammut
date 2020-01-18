package io.github.koss.paging.local

import kotlinx.coroutines.flow.Flow

interface LocalDataSource<LocalModel> {

    val localData: Flow<List<LocalModel>>

    suspend fun insertOrUpdate(vararg model: LocalModel)

    suspend fun insertOrUpdateAll(models: List<LocalModel>)

    suspend fun clearAllAndInsert(models: List<LocalModel>)
}
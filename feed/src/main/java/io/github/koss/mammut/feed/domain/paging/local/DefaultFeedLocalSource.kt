package io.github.koss.mammut.feed.domain.paging.local

import io.github.koss.mammut.data.database.dao.StatusDao
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.paging.local.LocalDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class DefaultFeedLocalSource(
        private val statusDao: StatusDao
): LocalDataSource<Status> {

    override val localData: Flow<List<Status>>
        get() = statusDao.getAllFlow()

    override suspend fun insertOrUpdate(vararg model: Status) = coroutineScope {
        statusDao.insertAll(model.toList())
    }

    override suspend fun insertOrUpdateAll(models: List<Status>) {
        statusDao.insertAll(models)
    }
}
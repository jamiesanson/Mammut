package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.sys1yagi.mastodon4j.api.Range
import io.github.jamiesanson.mammut.data.converters.toEntity
import io.github.jamiesanson.mammut.data.database.MammutDatabase
import io.github.jamiesanson.mammut.data.database.dao.StatusDao
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import javax.inject.Inject
import javax.inject.Named

class FeedViewModel @Inject constructor(
        @FeedScope
        private val feedPagingManager: FeedPagingManager,
        @FeedScope
        @Named("in_memory_feed_db")
        private val statusDao: StatusDao
) : ViewModel() {

    val results: Deferred<LiveData<PagedList<Status>>> = async {
        initialise()
        return@async LivePagedListBuilder(statusDao.getAllPaged(), 50).build()
    }

    private suspend fun initialise() {
        val earliest = statusDao.getEarliest()
        val latest = statusDao.getLatest()

        feedPagingManager.initialise(Range(sinceId = earliest?.id, maxId = latest?.id),
                getEarliestId = {
                    statusDao.getEarliest()?.id
                },
                getLatestId = {
                    statusDao.getLatest()?.id
                })

        withContext(UI) {
            feedPagingManager.feedResults.observeForever { newPage ->
                launch {
                    statusDao.insertNewPage(newPage.map { it.toEntity() })
                }
            }
        }
    }

}
package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.sys1yagi.mastodon4j.api.Handler
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.Shutdownable
import com.sys1yagi.mastodon4j.api.entity.Notification
import io.github.jamiesanson.mammut.data.converters.toEntity
import io.github.jamiesanson.mammut.data.database.dao.StatusDao
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.feature.base.Event
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedScope
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.StreamingBuilder
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import javax.inject.Inject
import javax.inject.Named

class FeedViewModel @Inject constructor(
        @FeedScope
        private val feedPagingManager: FeedPagingManager,
        @FeedScope
        @Named("in_memory_feed_db")
        private val statusDao: StatusDao,
        @FeedScope
        private val streamingBuilder: StreamingBuilder?
) : ViewModel() {

    val results: Deferred<LiveData<PagedList<Status>>> = async {
        initialise()
        return@async LivePagedListBuilder(statusDao.getAllPaged(), 50).build()
    }

    val errors: LiveData<Event<String>> = MutableLiveData()

    val onStreamedResult: LiveData<Event<Unit?>> = MutableLiveData()

    private var shutdownable: Shutdownable? = null

    private var streamStartJob: Job? = null

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

            feedPagingManager.errors.observeForever {
                errors.postSafely(Event(it))
            }
        }
    }

    fun startStreaming() {
        streamingBuilder ?: return
        if (shutdownable != null || streamStartJob != null) return

        streamStartJob = launch {
            shutdownable = streamingBuilder.startStream(object : Handler {
                override fun onDelete(id: Long) {
                    launch {
                        statusDao.deleteById(id)
                    }
                }

                override fun onNotification(notification: Notification) {
                    // No op for now
                }

                override fun onStatus(status: com.sys1yagi.mastodon4j.api.entity.Status) {
                    launch {
                        onStreamedResult.postSafely(Event(null))
                        statusDao.insertStatus(status.toEntity())
                    }
                }

            })
        }
    }

    fun stopStreaming() {
        streamStartJob?.cancel()
        shutdownable?.shutdown()
        shutdownable = null
        streamStartJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
    }

}
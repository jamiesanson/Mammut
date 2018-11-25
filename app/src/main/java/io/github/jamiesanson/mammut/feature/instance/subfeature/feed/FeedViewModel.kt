package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import io.github.jamiesanson.mammut.data.repo.PreferencesRepository
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.feature.base.Event
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedScope
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.StreamingBuilder
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Named

class FeedViewModel @Inject constructor(
        @FeedScope
        private val feedPagingManager: FeedPagingManager,
        @FeedScope
        @Named("in_memory_feed_db")
        private val statusDao: StatusDao,
        @FeedScope
        private val streamingBuilder: StreamingBuilder?,
        private val preferencesRepository: PreferencesRepository
) : ViewModel(), CoroutineScope by CoroutineScope(Dispatchers.Default) {

    val results: Deferred<LiveData<PagedList<Status>>> = async {
        initialise()
        return@async LivePagedListBuilder(statusDao.getAllPaged(), 50).build()
    }

    val errors: LiveData<Event<String>> = MutableLiveData()

    val onStreamedResult: LiveData<Event<Unit?>> = MutableLiveData()

    val refreshed: LiveData<Event<Boolean>> = MutableLiveData()

    private var shutdownable: Shutdownable? = null

    fun loadAround(id: Long) {
        feedPagingManager.loadAroundId(id)
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

        withContext(Dispatchers.Main) {
            feedPagingManager.feedResults.observeForever { newPage ->
                launch(Dispatchers.IO) {
                    statusDao.insertNewPage(newPage.map { it.toEntity() })
                }
            }

            feedPagingManager.errors.observeForever {
                errors.postSafely(Event(it))
            }
        }
    }

    fun startStreaming(): Boolean {
        if (!preferencesRepository.isStreamingEnabled) {
            // Cancel streaming if applicable
            shutdownable?.shutdown()
            shutdownable = null
            return false
        }

        streamingBuilder ?: return false
        if (shutdownable != null) return false

        launch {
            shutdownable = streamingBuilder.startStream(object : Handler {
                override fun onDelete(id: Long) {
                    statusDao.deleteById(id)
                }

                override fun onNotification(notification: Notification) {
                    // No op for now
                }

                override fun onStatus(status: com.sys1yagi.mastodon4j.api.entity.Status) {
                    onStreamedResult.postSafely(Event(null))
                    statusDao.insertStatus(status.toEntity())
                }
            })
        }

        return true
    }

    fun refresh() {
        launch {
            feedPagingManager.loadAroundIdSuspending(statusDao.getLatest()?.id ?: 0)
            refreshed.postSafely(Event(true))
        }
    }

    private fun stopStreaming() {
        shutdownable?.shutdown()
        shutdownable = null
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
    }

}
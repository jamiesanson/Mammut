package io.github.koss.mammut.feature.instance.subfeature.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.extension.awaitFirst
import io.github.koss.mammut.feature.base.Event
import io.github.koss.mammut.feature.feedpaging.*
import io.github.koss.mammut.base.dagger.scope.FeedScope
import kotlinx.coroutines.*
import javax.inject.Inject

class FeedViewModel @Inject constructor(
        @FeedScope
        private val feedPager: FeedPager,
        @FeedScope
        private val feedStateData: FeedStateData
) : ViewModel(), CoroutineScope by CoroutineScope(Dispatchers.Default) {

    val feedData: FeedData<Status> = feedPager.initialise()

    val onStreamedResult: LiveData<Event<List<com.sys1yagi.mastodon4j.api.entity.Status>>>
        get() = feedData.itemStreamed

    val shouldScrollOnFirstLoad: Boolean = feedPager.getPreviousPosition() == null

    init {
        feedStateData.observableState.observeForever {
            when (it) {
                FeedState.PagingUpwards -> {
                    launch {
                        val list = feedData.pagedList.awaitFirst()
                        list.firstOrNull()?.let(feedPager::forceLoadAtFront)
                    }
                }
            }
        }
    }

    fun refresh() {
       feedData.refresh()
    }

    fun onBrokenTimelineResolved() {
        feedStateData.store.send(FeedStateEvent.OnBrokenTimelineResolved)
    }

    fun getPreviousPosition(): Int? = feedPager.getPreviousPosition()

    fun savePageState(position: Int) {
        feedPager.onCleared(position)
    }

    override fun onCleared() {
        super.onCleared()
        feedPager.tearDown()
    }
}
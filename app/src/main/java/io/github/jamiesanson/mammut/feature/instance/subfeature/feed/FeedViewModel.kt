package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.feature.base.Event
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedScope
import io.github.jamiesanson.mammut.feature.feedpaging.FeedData
import io.github.jamiesanson.mammut.feature.feedpaging.FeedPager
import io.github.jamiesanson.mammut.feature.feedpaging.FeedStateData
import io.github.jamiesanson.mammut.feature.feedpaging.FeedStateEvent
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
}
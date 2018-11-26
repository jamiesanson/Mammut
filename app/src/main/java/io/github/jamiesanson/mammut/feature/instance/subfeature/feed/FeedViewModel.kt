package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.feature.base.Event
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedScope
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.paging.FeedData
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.paging.FeedPagingHelper
import kotlinx.coroutines.*
import javax.inject.Inject

class FeedViewModel @Inject constructor(
        @FeedScope
        private val feedPagingHelper: FeedPagingHelper
) : ViewModel(), CoroutineScope by CoroutineScope(Dispatchers.Default) {

    val feedData: FeedData<Status> = feedPagingHelper.initialise()

    val onStreamedResult: LiveData<Event<Unit?>> = feedData.itemStreamed

    val shouldScrollOnFirstLoad: Boolean = feedPagingHelper.getPreviousPosition() == null

    fun refresh() {
       feedData.refresh()
    }

    fun getPreviousPosition(): Int? = feedPagingHelper.getPreviousPosition()

    fun savePageState(position: Int) {
        feedPagingHelper.onCleared(position)
    }
}
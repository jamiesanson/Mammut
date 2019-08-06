package io.github.koss.mammut.feed.domain

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.domain.paging.FeedPagingManager
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class FeedViewModel @Inject constructor(
        private val pagingManager: FeedPagingManager
): ViewModel() {

    init {
        pagingManager.activate()
    }

    val feedData: LiveData<List<Status>> = liveData {
        pagingManager.data
                .collect {
                    emit(it)
                }
    }

    override fun onCleared() {
        super.onCleared()
        pagingManager.deactivate()
    }
}
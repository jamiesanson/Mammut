package io.github.koss.mammut.feed.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.domain.paging.FeedPagingManager
import io.github.koss.paging.network.LoadingState
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class FeedViewModel @Inject constructor(
        private val pagingManager: FeedPagingManager
): ViewModel() {

    init {
        pagingManager.activate()
    }

    /**
     * Expose the list of Statuses from the paging manager
     * TODO - Change this to emit a ViewModel-type model, such that broken timeline logic can be kept here
     */
    val feedData: LiveData<List<Status>> = liveData {
        pagingManager.data
                .collect {
                    Log.d("FeedViewModel", "Recieved list $it")
                    emit(it)
                }
    }

    /**
     * Expose the loading state of the page manager
     */
    val loadingState: LiveData<LoadingState> = liveData {
        pagingManager.loadingState
                .collect {
                    emit(it)
                }
    }

    override fun onCleared() {
        super.onCleared()
        pagingManager.deactivate()
    }
}
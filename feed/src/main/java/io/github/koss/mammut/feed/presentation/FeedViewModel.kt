package io.github.koss.mammut.feed.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.github.koss.mammut.feed.domain.paging.FeedPagingManager
import io.github.koss.mammut.feed.presentation.state.FeedReducer
import io.github.koss.mammut.feed.presentation.state.FeedState
import io.github.koss.mammut.feed.presentation.state.LoadingAll
import io.github.koss.mammut.feed.presentation.state.OnItemsLoaded
import io.github.koss.randux.applyMiddleware
import io.github.koss.randux.createStore
import io.github.koss.randux.utils.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FeedViewModel @Inject constructor(
        private val pagingManager: FeedPagingManager
): ViewModel() {

    private val store: Store = createStore(
        reducer = FeedReducer(),
        enhancer = applyMiddleware(),
        preloadedState = LoadingAll)

    init {
        pagingManager.activate()

        // Publish state
        store.subscribe {
            (store.getState() as? FeedState)?.let {
                stateRelay.offer(it)
            }
        }

        // Observe Paging data
        viewModelScope.launch {
            @Suppress("EXPERIMENTAL_API_USAGE")
            pagingManager.data
                .zip(pagingManager.loadingState) { items, loading ->
                    items to loading
                }
                .collect { (items, loading) ->
                    withContext(Dispatchers.Main) {
                        store.dispatch(OnItemsLoaded(items, loading))
                    }
                }
        }

    }

    private val stateRelay = Channel<FeedState>(capacity = CONFLATED)

    val state = liveData {
        for (item in stateRelay) {
            emit(item)
        }
    }

    fun reload() {

    }

    override fun onCleared() {
        super.onCleared()
        pagingManager.deactivate()
    }
}
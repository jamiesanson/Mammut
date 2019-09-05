package io.github.koss.mammut.feed.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.github.koss.mammut.feed.domain.paging.FeedPagingManager
import io.github.koss.mammut.feed.presentation.state.FeedReducer
import io.github.koss.mammut.feed.presentation.state.FeedState
import io.github.koss.mammut.feed.presentation.state.LoadingAll
import io.github.koss.mammut.feed.presentation.state.OnItemsLoaded
import io.github.koss.mammut.feed.presentation.state.OnLoadingStateChanged
import io.github.koss.mammut.feed.presentation.status.StatusRenderingMiddleware
import io.github.koss.randux.applyMiddleware
import io.github.koss.randux.createStore
import io.github.koss.randux.utils.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FeedViewModel @Inject constructor(
    applicationContext: Context,
    private val pagingManager: FeedPagingManager
) : ViewModel() {

    private val store: Store = createStore(
        reducer = FeedReducer(),
        enhancer = applyMiddleware(StatusRenderingMiddleware(viewModelScope, applicationContext)),
        preloadedState = LoadingAll
    )

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
                .collect { items ->
                    withContext(Dispatchers.Main) {
                        store.dispatch(OnItemsLoaded(items))
                    }
                }

            pagingManager.loadingState
                .collect {
                    withContext(Dispatchers.Main) {
                        store.dispatch(OnLoadingStateChanged(it))
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

    fun savePageState(position: Int) {
        // TODO - Save position in persistence
    }

    fun reload() {
        // TODO - Process full reload
    }

    override fun onCleared() {
        super.onCleared()
        pagingManager.deactivate()
    }
}
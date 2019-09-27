package io.github.koss.mammut.feed.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.github.koss.mammut.feed.domain.paging.FeedPagingManager
import io.github.koss.mammut.feed.presentation.event.FeedEvent
import io.github.koss.mammut.feed.presentation.state.FeedReducer
import io.github.koss.mammut.feed.presentation.state.FeedState
import io.github.koss.mammut.feed.presentation.state.LoadingAll
import io.github.koss.mammut.feed.presentation.state.OnItemsLoaded
import io.github.koss.mammut.feed.presentation.state.OnLoadingStateChanged
import io.github.koss.mammut.feed.presentation.status.StatusRenderingMiddleware
import io.github.koss.paging.event.ItemStreamed
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
        setupStore()
        setupPaging()
    }

    private val stateRelay = Channel<FeedState>(capacity = CONFLATED)

    private val eventRelay = Channel<FeedEvent>(capacity = CONFLATED)

    val state = liveData {
        for (item in stateRelay) {
            emit(item)
        }
    }

    val event = liveData {
        for (item in eventRelay) {
            emit(item)
        }
    }

    fun savePageState(position: Int) {
        // TODO - Save position in persistence
    }

    fun reload() {
        pagingManager.reload()
    }

    private fun setupStore() {
        // Publish state
        store.subscribe {
            (store.getState() as? FeedState)?.let {
                stateRelay.offer(it)
            }
        }
    }

    private fun setupPaging() {
        pagingManager.activate()

        // Observe Paging data
        viewModelScope.launch {
            launch {
                @Suppress("EXPERIMENTAL_API_USAGE")
                pagingManager.data
                    .collect { items ->
                        withContext(Dispatchers.Main) {
                            store.dispatch(OnItemsLoaded(items))
                        }
                    }
            }

            launch {
                pagingManager.loadingState
                    .collect {
                        withContext(Dispatchers.Main) {
                            store.dispatch(OnLoadingStateChanged(it))
                        }
                    }
            }

            // Listen to paging events
            launch {
                for (event in pagingManager.pagingRelay.openSubscription()) {
                    when (event) {
                        is ItemStreamed ->
                            eventRelay.offer(io.github.koss.mammut.feed.presentation.event.ItemStreamed)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pagingManager.deactivate()
    }
}
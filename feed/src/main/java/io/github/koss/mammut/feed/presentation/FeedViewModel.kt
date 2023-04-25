package io.github.koss.mammut.feed.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.github.koss.mammut.base.navigation.Event
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.feed.domain.paging.FeedPagingManager
import io.github.koss.mammut.feed.domain.preferences.PreferencesRepository
import io.github.koss.mammut.feed.presentation.event.FeedEvent
import io.github.koss.mammut.feed.presentation.event.Navigation
import io.github.koss.mammut.feed.presentation.state.FeedReducer
import io.github.koss.mammut.feed.presentation.state.FeedState
import io.github.koss.mammut.feed.presentation.state.LoadingAll
import io.github.koss.mammut.feed.presentation.state.OnItemsLoaded
import io.github.koss.mammut.feed.presentation.state.OnLoadingStateChanged
import io.github.koss.mammut.feed.presentation.status.SpanReplacingMiddleware
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
    private val feedType: FeedType,
    private val pagingManager: FeedPagingManager,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val store: Store = createStore(
        reducer = FeedReducer(),
        enhancer = applyMiddleware(
                StatusRenderingMiddleware(viewModelScope, applicationContext),
                SpanReplacingMiddleware(::onNavigationEvent)
        ),
        preloadedState = LoadingAll(
            initialPosition = when (feedType) {
                FeedType.Home -> preferencesRepository.homeFeedLastPositionSeen
                FeedType.Local -> preferencesRepository.localFeedLastPositionSeen
                else -> -1
            }
        )
    )

    private val stateRelay = Channel<FeedState>(capacity = CONFLATED)

    private val eventRelay = Channel<FeedEvent>(capacity = CONFLATED)

    init {
        setupStore()
        setupPaging()
    }

    val state = liveData {
        for (item in stateRelay) {
            emit(item)
        }
    }

    val event = liveData {
        for (item in eventRelay) {
            emit(Event(item))
        }
    }

    fun savePageState(position: Int) {
        when (feedType) {
            FeedType.Home -> preferencesRepository.homeFeedLastPositionSeen = position
            FeedType.Local -> preferencesRepository.localFeedLastPositionSeen = position
            else -> Unit
        }
    }

    fun reload() {
        pagingManager.reload()
    }

    private fun setupStore() {
        // Publish state
        store.subscribe {
            (store.getState() as? FeedState)?.let {
                stateRelay.trySend(it)
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
                pagingManager.pagingRelay.collect { event ->
                    if (event is ItemStreamed) {
                        eventRelay.trySend(io.github.koss.mammut.feed.presentation.event.ItemStreamed)
                    }
                }
            }
        }
    }

    private fun onNavigationEvent(event: Navigation) {
        viewModelScope.launch {
            eventRelay.trySend(event)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pagingManager.deactivate()
    }
}
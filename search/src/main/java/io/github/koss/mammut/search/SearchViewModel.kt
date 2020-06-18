package io.github.koss.mammut.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.method.Public
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.search.presentation.state.NoResults
import io.github.koss.mammut.search.presentation.state.OnLoadStart
import io.github.koss.mammut.search.presentation.state.OnResults
import io.github.koss.mammut.search.presentation.state.SearchReducer
import io.github.koss.mammut.search.presentation.state.SearchState
import io.github.koss.randux.applyMiddleware
import io.github.koss.randux.createStore
import io.github.koss.randux.utils.Store
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val client: MastodonClient
): ViewModel() {

    private val store: Store = createStore(
        reducer = SearchReducer(),
        preloadedState = NoResults(query = "")
    )

    private val stateRelay = Channel<SearchState>(capacity = Channel.CONFLATED)

    private var queryDebounceJob: Job? = null

    val state = liveData {
        for (item in stateRelay) {
            emit(item)
        }
    }

    init {
        setupStore()
    }

    private fun setupStore() {
        // Publish state
        store.subscribe {
            (store.getState() as? SearchState)?.let {
                stateRelay.offer(it)
            }
        }
    }

    fun search(query: String) {
        if (state.value?.query == query) {
            return
        }

        queryDebounceJob?.cancel()

        queryDebounceJob = viewModelScope.launch {
            store.dispatch(OnLoadStart(query))

            delay(1000)

            val results = async(Dispatchers.IO) {
                Public(client).getSearch(query = query, resolve = true).execute()
            }

            store.dispatch(OnResults(query, results.await()))
        }
    }
}
package io.github.jamiesanson.mammut.feature.feedpaging

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.jamiesanson.mammut.feature.base.Event

data class FeedData<T>(
        // the LiveData of paged lists for the UI to observe
        val pagedList: LiveData<PagedList<T>>,
        // The state of the feed
        val state: LiveData<FeedState>,
        // notifies when items are streamed
        val itemStreamed: LiveData<Event<List<Status>>>,
        // represents the network request status to show to the user
        val networkState: LiveData<NetworkState>,
        // represents the refresh status to show to the user. Separate from networkState, this
        // value is importantly only when refresh is requested.
        val refreshState: LiveData<NetworkState>,
        // refreshes the whole data and fetches it from scratch.
        val refresh: () -> Unit,
        // retries any failed requests.
        val retry: () -> Unit)
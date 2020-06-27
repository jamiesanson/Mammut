package io.github.koss.mammut.feed.domain.paging.network

import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Shutdownable
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.paging.network.NetworkDataSource
import io.github.koss.paging.network.StreamingSupportedDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StreamingSupportedFeedNetworkSource(
        defaultSource: DefaultFeedNetworkSource,
        private val feedType: FeedType,
        private val client: MastodonClient
): StreamingSupportedDataSource<Status>, NetworkDataSource<Status> by defaultSource {

    private var shutdownable: Shutdownable? = null

    private val resultsRelay = Channel<Status>()

    override val streamedResults: Flow<Status> = flow {
        for (result in resultsRelay) {
            emit(result)
        }
    }

    override suspend fun activate() = coroutineScope {
        val streamBuilder = feedType.getStreamingBuilder(client) ?: return@coroutineScope
        val resultHandler = PublicStreamHandler {
            resultsRelay.offer(it)
        }

        shutdownable = streamBuilder(resultHandler)
    }

    override suspend fun deactivate() = coroutineScope {
        shutdownable?.shutdown()
        super.deactivate()
    }

}
package io.github.koss.mammut.feed.domain.paging

import io.github.koss.mammut.data.converters.toDomainModel
import io.github.koss.mammut.data.converters.toLocalModel
import io.github.koss.mammut.data.converters.toNetworkModel
import io.github.koss.mammut.data.models.Status
import io.github.koss.paging.PagingManager
import io.github.koss.paging.default.DefaultPagingManager
import io.github.koss.paging.event.PagingRelay
import io.github.koss.paging.local.LocalDataSource
import io.github.koss.paging.network.NetworkDataSource
import kotlinx.coroutines.CoroutineScope

typealias NetworkStatus = com.sys1yagi.mastodon4j.api.entity.Status
typealias LocalStatus = io.github.koss.mammut.data.database.entities.feed.Status

/**
 * Paging manager for a feed of statuses. This is used in places like Home, Local and Federated timelines,
 * as well as profile feeds.
 */
class FeedPagingManager(
        scope: CoroutineScope,
        pagingRelay: PagingRelay,
        localDataSource: LocalDataSource<LocalStatus>,
        networkDataSource: NetworkDataSource<NetworkStatus>
) : PagingManager<LocalStatus, NetworkStatus, Status> by DefaultPagingManager(scope, pagingRelay, localDataSource, networkDataSource) {

    override fun localToDomain(local: LocalStatus): Status =
            local.toDomainModel()

    override fun networkToLocal(network: NetworkStatus): LocalStatus =
            network.toLocalModel()

    override fun domainToNetwork(domain: Status): NetworkStatus =
            domain.toNetworkModel()
}
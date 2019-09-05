package io.github.koss.paging.default

import io.github.koss.paging.*
import io.github.koss.paging.event.*
import io.github.koss.paging.local.LocalDataSource
import io.github.koss.paging.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.*

/**
 * Default implementation of a paging manager, which can be used via delegation. By default, this
 * assumes all paging is to be backed with a local data source. It calls out to network data sources
 * based off data fed in via the PagingRelay
 */
class DefaultPagingManager<LocalModel, NetworkModel, DomainModel>(
        scope: CoroutineScope,
        override val pagingRelay: PagingRelay,
        override val localDataSource: LocalDataSource<LocalModel>,
        override val networkDataSource: NetworkDataSource<NetworkModel>,
        override val pagingMapper: PagingMapper<LocalModel, NetworkModel, DomainModel>
) : PagingManager<LocalModel, NetworkModel, DomainModel>, CoroutineScope by scope {

    private var activatedJob: Job? = null

    private val loadParentJob: Job = Job()

    private val loadRelay: Channel<LoadingState> = Channel(CONFLATED)

    override val data: Flow<List<DomainModel>>
        get() = localDataSource.localData.map { it.map(pagingMapper::localToDomain) }

    override val loadingState: Flow<LoadingState> = flow {
        for (loadingState in loadRelay) {
            emit(loadingState)
        }
    }

    override fun activate() {
        activatedJob = launch {
            // Observe events
            launch {
                for (event in pagingRelay.openSubscription()) {
                    launch(loadParentJob) {
                        when (event) {
                            NoDataDisplayed -> loadFresh()
                            DataStartReached -> loadAtFront()
                            DataEndReached -> loadAtEnd()
                        }
                    }
                }
            }

            // Observe streamed results
            if (networkDataSource is StreamingSupportedDataSource<*>) {
                networkDataSource.activate()

                launch {
                    @Suppress("UNCHECKED_CAST")
                    (networkDataSource.streamedResults as Flow<NetworkModel>)
                            .map { pagingMapper.networkToLocal(it) }
                            .onEach {
                                localDataSource.insertOrUpdateAll(listOf(it))
                                pagingRelay.onItemStreamed()
                            }
                            .collect()
                }
            }

            // Kick off the initial load
            loadFresh()
        }
    }

    override fun deactivate() {
        launch {
            (networkDataSource as? StreamingSupportedDataSource<*>)?.deactivate()
        }

        activatedJob?.cancel()
        activatedJob = null
    }

    private suspend fun loadFresh() = performLoad(LoadingAll) {
        doLoad(Initial())
    }

    private suspend fun loadAtFront() = performLoad(LoadingAtFront) {
        doLoad(Before(pagingMapper.domainToNetwork(data.first().first())))
    }

    private suspend fun loadAtEnd() = performLoad(LoadingAtEnd) {
        doLoad(After(pagingMapper.domainToNetwork(data.first().last())))
    }

    private suspend fun doLoad(config: LoadConfig<NetworkModel>) {
        val newData = networkDataSource.loadMore(config)
        val localData = newData.map(pagingMapper::networkToLocal)
        localDataSource.insertOrUpdateAll(localData)
    }

    private suspend fun performLoad(loadState: LoadingState, block: suspend () -> Unit) = coroutineScope {
        loadRelay.send(loadState)
        block()

        // If there's only one job in the list, it's this one. Finish the load.
        if (loadParentJob.children.count { it.isActive } == 1) {
            loadRelay.send(NotLoading)
        }
    }
}
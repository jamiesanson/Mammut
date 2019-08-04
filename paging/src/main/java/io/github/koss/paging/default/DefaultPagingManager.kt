package io.github.koss.paging.default

import io.github.koss.paging.*
import io.github.koss.paging.event.DataEndReached
import io.github.koss.paging.event.DataStartReached
import io.github.koss.paging.event.NoDataDisplayed
import io.github.koss.paging.event.PagingRelay
import io.github.koss.paging.local.LocalDataSource
import io.github.koss.paging.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
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
        override val networkDataSource: NetworkDataSource<NetworkModel>
) : PagingManager<LocalModel, NetworkModel, DomainModel>, CoroutineScope by scope {

    private var activatedJob: Job? = null

    private val loadRelay: Channel<LoadingState> = Channel()

    override val data: Flow<List<DomainModel>>
        get() = localDataSource.localData.map { it.map(::localToDomain) }

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
                    when (event) {
                        NoDataDisplayed -> launch { loadFresh() }
                        DataStartReached -> launch { loadAtFront() }
                        DataEndReached -> launch { loadAtEnd() }
                    }
                }
            }

            // Observe streamed results
            if (networkDataSource is StreamingSupportedDataSource<*>) {
                launch {
                    networkDataSource
                            .streamedResults
                            .map {
                                @Suppress("UNCHECKED_CAST")
                                it as? NetworkModel
                            }
                            .onEach {
                                it?.let {
                                    localDataSource.insertOrUpdate(networkToLocal(it))
                                }
                            }
                            .collect()

                    networkDataSource.activate()
                }
            }
        }
    }

    override fun deactivate() {
        launch {
            (networkDataSource as? StreamingSupportedDataSource<*>)?.deactivate()
        }

        activatedJob?.cancel()
        activatedJob = null
    }

    private suspend fun loadFresh() = coroutineScope {
        loadRelay.send(LoadingAll)
        doLoad(Initial())
    }

    private suspend fun loadAtFront() = coroutineScope {
        loadRelay.send(LoadingAtFront)
        doLoad(Before(domainToNetwork(data.first().first())))
    }

    private suspend fun loadAtEnd() = coroutineScope {
        loadRelay.send(LoadingAtEnd)
        doLoad(After(domainToNetwork(data.first().last())))
    }

    private suspend fun doLoad(config: LoadConfig<NetworkModel>) {
        val newData = networkDataSource.loadMore(config)
        val localData = newData.map(::networkToLocal)
        localDataSource.insertOrUpdateAll(localData)
    }

    override fun localToDomain(local: LocalModel): DomainModel {
        TODO("""
            PagingManager::localToDomain not implemented.
            
            -----------------------------------------------------------------------------
            This occurs when you use class delegation to delegate to DefaultPagingManager.
            To fix this, override localToDomain in your paging manager implementation.
            -----------------------------------------------------------------------------
        """.trimIndent())
    }

    override fun networkToLocal(network: NetworkModel): LocalModel {
        TODO("""
            PagingManager::networkToLocal not implemented.
            
            -----------------------------------------------------------------------------
            This occurs when you use class delegation to delegate to DefaultPagingManager.
            To fix this, override networkToLocal in your paging manager implementation.
            -----------------------------------------------------------------------------
        """.trimIndent())
    }

    override fun domainToNetwork(domain: DomainModel): NetworkModel {
        TODO("""
            PagingManager::domainToNetwork not implemented.
            
            -----------------------------------------------------------------------------
            This occurs when you use class delegation to delegate to DefaultPagingManager.
            To fix this, override domainToNetwork in your paging manager implementation.
            -----------------------------------------------------------------------------
        """.trimIndent())
    }

}
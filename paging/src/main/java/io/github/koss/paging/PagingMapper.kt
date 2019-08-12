package io.github.koss.paging

/**
 * Interface defining a component which handles mapping between various model types
 */
interface PagingMapper<LocalModel, NetworkModel, DomainModel> {

    fun localToDomain(local: LocalModel): DomainModel

    fun networkToLocal(network: NetworkModel): LocalModel

    fun domainToNetwork(domain: DomainModel): NetworkModel
}
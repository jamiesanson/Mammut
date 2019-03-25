package io.github.koss.mammut.dagger.module

import dagger.Module
import dagger.Provides
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.repository.InstanceDetailRepository
import io.github.koss.mammut.data.repository.InstancesRepository
import io.github.koss.mammut.extension.ClientBuilder

@Module
class RepositoryModule {

    @Provides
    @ApplicationScope
    fun provideInstanceDetailRepository(database: MammutDatabase, instancesRepository: InstancesRepository, clientBuilder: ClientBuilder): InstanceDetailRepository {
        val builder = { name: String -> clientBuilder.getInstanceBuilder(name).build() }

        return InstanceDetailRepository(
                mammutDatabase = database,
                instancesRepository = instancesRepository,
                clientBuilder = builder)
    }

    @Provides
    @ApplicationScope
    fun provideInstancesRepository(database: MammutDatabase): InstancesRepository {
        return InstancesRepository(database)
    }
}
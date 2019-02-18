package io.github.koss.mammut.dagger.module

import dagger.Module
import dagger.Provides
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.repository.InstanceDetailRepository
import io.github.koss.mammut.extension.ClientBuilder

@Module
class RepositoryModule {

    @Provides
    @ApplicationScope
    fun provideInstanceDetailRepository(database: MammutDatabase, clientBuilder: ClientBuilder): InstanceDetailRepository {
        val builder = { name: String -> clientBuilder.getInstanceBuilder(name).build() }

        return InstanceDetailRepository(
                mammutDatabase = database,
                clientBuilder = builder)
    }
}
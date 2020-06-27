package io.github.koss.mammut.toot.dagger

import com.sys1yagi.mastodon4j.MastodonClient
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.data.extensions.ClientBuilder
import io.github.koss.mammut.data.repository.InstanceDetailRepository
import io.github.koss.mammut.toot.BuildConfig
import io.github.koss.mammut.toot.repo.StatusRepository
import javax.inject.Named

@Module(includes = [ComposeTootViewModelModule::class])
class ComposeTootModule(private val instanceName: String, private val accessToken: String) {

    @Provides
    @ComposeTootScope
    @Named("compose_toot_client")
    fun provideClient(clientBuilder: ClientBuilder): MastodonClient {
        return clientBuilder.getInstanceBuilder(instanceName)
                .accessToken(accessToken)
                .apply {
                    if (BuildConfig.DEBUG) {
                        debug()
                    }
                }
                .build()
    }

    @Provides
    @ComposeTootScope
    fun provideStatusRepository(
            @Named("compose_toot_client") client: MastodonClient,
            instanceDetailRepository: InstanceDetailRepository
    ): StatusRepository {
        return StatusRepository(client, instanceDetailRepository)
    }
}
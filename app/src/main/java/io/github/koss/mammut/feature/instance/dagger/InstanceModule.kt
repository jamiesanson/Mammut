package io.github.koss.mammut.feature.instance.dagger

import com.sys1yagi.mastodon4j.MastodonClient
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.BuildConfig
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.data.extensions.ClientBuilder
import javax.inject.Named

@Module
class InstanceModule(private val instanceName: String, private val accessToken: String) {

    @Provides
    @InstanceScope
    fun provideAuthenticatedClient(clientBuilder: ClientBuilder): MastodonClient {
        return clientBuilder.getInstanceBuilder(instanceName)
                .accessToken(accessToken)
                .useStreamingApi()
                .apply {
                    if (BuildConfig.DEBUG) {
                        debug()
                    }
                }
                .build()
    }

    @Provides
    @InstanceScope
    @Named("instance_name")
    fun provideInstanceName(): String = instanceName

    @Provides
    @InstanceScope
    @Named("instance_access_token")
    fun provideInstanceAccessToken(): String = accessToken
}
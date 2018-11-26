package io.github.jamiesanson.mammut.feature.instance.dagger

import androidx.recyclerview.widget.RecyclerView
import com.sys1yagi.mastodon4j.MastodonClient
import dagger.Module
import dagger.Provides
import io.github.jamiesanson.mammut.BuildConfig
import io.github.jamiesanson.mammut.extension.ClientBuilder
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

    @Provides
    @InstanceScope
    fun provideFeedViewPool(): RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool()
}
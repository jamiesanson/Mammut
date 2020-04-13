package io.github.koss.mammut.dagger.module

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.base.dagger.scope.ApplicationScope
import io.github.koss.mammut.data.extensions.ClientBuilder
import okhttp3.OkHttpClient

@Module
class NetworkModule {

    @Provides
    @ApplicationScope
    fun provideOkHttpClientBuilder(): OkHttpClient.Builder =
            OkHttpClient.Builder()

    @Provides
    @ApplicationScope
    fun provideGsonBuilder(): GsonBuilder = GsonBuilder()

    @Provides
    @ApplicationScope
    fun provideClientBuilder(okHttpClient: OkHttpClient.Builder, gson: GsonBuilder): ClientBuilder = ClientBuilder(okHttpClient, gson)
}
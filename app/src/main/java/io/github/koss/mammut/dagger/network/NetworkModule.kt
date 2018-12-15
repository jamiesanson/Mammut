package io.github.jamiesanson.mammut.dagger.network

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import io.github.jamiesanson.mammut.dagger.application.ApplicationScope
import io.github.jamiesanson.mammut.extension.ClientBuilder
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
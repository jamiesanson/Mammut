package io.github.jamiesanson.mammut.dagger.network

import com.google.gson.Gson
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
    fun provideGson(): Gson = Gson()

    @Provides
    @ApplicationScope
    fun provideClientBuilder(okHttpClient: OkHttpClient.Builder, gson: Gson): ClientBuilder = ClientBuilder(okHttpClient, gson)
}
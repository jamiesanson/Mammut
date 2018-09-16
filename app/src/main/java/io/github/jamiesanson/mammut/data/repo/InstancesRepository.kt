package io.github.jamiesanson.mammut.data.repo

import io.github.jamiesanson.mammut.BuildConfig
import io.github.jamiesanson.mammut.dagger.application.ApplicationScope
import io.github.jamiesanson.mammut.data.remote.MastodonInstancesService
import io.github.jamiesanson.mammut.data.remote.response.InstanceDetail
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

private const val INSTANCES_URL = "https://instances.social/api/1.0/"

@ApplicationScope
class InstancesRepository @Inject constructor() {

    private val instancesService: MastodonInstancesService by lazy {
        Retrofit.Builder()
                .client(OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            chain.proceed(
                                    chain.request().newBuilder()
                                            .addHeader("Authorization", "Bearer ${BuildConfig.INSTANCES_SECRET}")
                                            .build())
                        }.build())
                .baseUrl(INSTANCES_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MastodonInstancesService::class.java)
    }

    suspend fun getInstanceInformation(name: String): InstanceDetail? {
        val response = instancesService.getInstanceInformation(name).execute()

        return if (response.isSuccessful) {
            response.body()
        } else {
            // TODO - Detailed error handling
            null
        }
    }

}
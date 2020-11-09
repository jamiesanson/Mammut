package io.github.koss.mammut.data.repository

import io.github.koss.mammut.data.BuildConfig
import io.github.koss.mammut.data.converters.toNetworkModel
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.database.entities.InstanceSearchResultEntity
import io.github.koss.mammut.data.models.InstanceSearchResult
import io.github.koss.mammut.instances.MastodonInstancesService
import io.github.koss.mammut.instances.response.InstanceDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val INSTANCES_URL = "https://instances.social/api/1.0/"

class InstancesRepository constructor(
        private val database: MammutDatabase
) {

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

    private suspend fun retrieveAllInstances(): List<InstanceDetail>? {
        val response = instancesService.getAllInstances(minActiveUsers = 20).execute()

        return if (response.isSuccessful) {
            response.body()?.instances
        } else {
            // TODO - Detailed error handling
            null
        }
    }

    suspend fun initialiseResults() = withContext(Dispatchers.IO) {
        if (database.instanceSearchDao().getAllResults().isEmpty()) {
            val instances = retrieveAllInstances() ?: return@withContext
            database.instanceSearchDao().addAllResults(instances.map { InstanceSearchResultEntity(name = it.name, users = it.users.toLong()) })
        }
    }

    suspend fun searchInstances(query: String): List<InstanceSearchResult> {
        return database.instanceSearchDao().getAllResults()
                .asSequence()
                .filter { it.name.contains(query, ignoreCase = true) }
                .distinctBy { it.name }
                .sortedByDescending { it.users }
                .map { it.toNetworkModel() }
                .toList()
    }
}
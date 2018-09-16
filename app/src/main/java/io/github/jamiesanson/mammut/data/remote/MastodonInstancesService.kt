package io.github.jamiesanson.mammut.data.remote

import io.github.jamiesanson.mammut.data.remote.response.InstanceDetail
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MastodonInstancesService {

    @GET("instances/show")
    fun getInstanceInformation(@Query("name") name: String): Call<InstanceDetail>
}
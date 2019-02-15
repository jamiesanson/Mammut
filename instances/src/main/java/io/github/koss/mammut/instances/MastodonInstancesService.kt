package io.github.koss.mammut.instances

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MastodonInstancesService {

    @GET("instances/show")
    fun getInstanceInformation(@Query("name") name: String): Call<InstanceDetail>

    @GET("instances/list")
    fun getAllInstances(
            @Query("count") count: Int = 0,
            @Query("include_closed") includeClosed: Boolean = false,
            @Query("include_down") includeDown: Boolean = false,
            @Query("min_active_users") minActiveUsers: Int = 50): Call<AllInstancesResponse>
}
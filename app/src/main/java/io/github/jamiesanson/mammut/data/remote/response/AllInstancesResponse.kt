package io.github.jamiesanson.mammut.data.remote.response

import com.google.gson.annotations.SerializedName

data class AllInstancesResponse(
        @SerializedName("instances")
        val instances: List<InstanceDetail>
)
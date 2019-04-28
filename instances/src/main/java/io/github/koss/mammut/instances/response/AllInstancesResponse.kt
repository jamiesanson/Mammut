package io.github.koss.mammut.instances.response

import com.google.gson.annotations.SerializedName

data class AllInstancesResponse(
        @SerializedName("instances")
        val instances: List<InstanceDetail>
)
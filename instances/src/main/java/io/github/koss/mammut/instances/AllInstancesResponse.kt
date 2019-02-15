package io.github.koss.mammut.instances

import com.google.gson.annotations.SerializedName

data class AllInstancesResponse(
        @SerializedName("instances")
        val instances: List<InstanceDetail>
)
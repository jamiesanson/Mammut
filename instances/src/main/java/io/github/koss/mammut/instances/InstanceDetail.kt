package io.github.koss.mammut.instances

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class InstanceDetail(
        val id: String,
        val name: String,
        val uptime: Float,
        val up: Boolean,
        val dead: Boolean,
        val version: String,
        val ipv6: Boolean,
        val users: String,
        val statuses: String,
        val connections: String,
        val thumbnail: String,
        @SerializedName("active_users")
        val activeUsers: Int,
        val info: Info
): Parcelable {

    @Parcelize
    data class Info(
            @SerializedName("short_description")
            val shortDescription: String,
            @SerializedName("full_description")
            val fullDescription: String,
            val languages: List<String>,
            @SerializedName("other_languages_accepted")
            val otherLanguagesAccepted: Boolean,
            @SerializedName("prohibited_content")
            val prohibitedContent: List<String>
    ): Parcelable
}

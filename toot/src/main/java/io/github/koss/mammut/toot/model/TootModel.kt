package io.github.koss.mammut.toot.model

import android.os.Parcelable
import com.sys1yagi.mastodon4j.api.entity.Status
import kotlinx.parcelize.Parcelize

/**
 * Model containing the required information for submitting a new [Status]
 */
@Parcelize
data class TootModel(
        val status: String,
        val inReplyToId: Long?,
        val mediaIds: List<Long>?,
        val sensitive: Boolean,
        val spoilerText: String?,
        val visibility: Status.Visibility = Status.Visibility.Public
): Parcelable
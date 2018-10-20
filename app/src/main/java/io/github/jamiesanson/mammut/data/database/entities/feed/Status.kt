package io.github.jamiesanson.mammut.data.database.entities.feed

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.jamiesanson.mammut.data.models.*

@Entity
data class Status @JvmOverloads constructor(
        @PrimaryKey
        val id: Long = 0L,
        val uri: String? = "",
        val url: String? = "",
        @Embedded
        val account: Account? = null,
        val inReplyToId: Long? = null,
        val inReplyToAccountId: Long? = null,
        val reblogId: Long = 0L,
        val content: String = "",
        val createdAt: String = "",
        @Embedded
        val emojis: ArrayList<Emoji>? = ArrayList(),
        val repliesCount: Int = 0,
        val reblogsCount: Int = 0,
        val favouritesCount: Int = 0,
        val isReblogged: Boolean = false,
        val isFavourited: Boolean = false,
        val isSensitive: Boolean = false,
        val spoilerText: String = "",
        val visibility: String = Visibility.Public.value,
        @Embedded
        val mediaAttachments: ArrayList<Attachment>? = ArrayList(),
        @Embedded
        val mentions: ArrayList<Mention>? = ArrayList(),
        @Embedded
        val tags: ArrayList<Tag>? = ArrayList(),
        @Embedded
        val application: Application? = null,
        val language: String? = null,
        val pinned: Boolean? = null) {
    enum class Visibility(val value: String) {
        Public("public"),
        Unlisted("unlisted"),
        Private("private"),
        Direct("direct")
    }
}
package io.github.koss.mammut.data.models

import com.sys1yagi.mastodon4j.api.entity.Attachment

data class Status(
        val id: Long = 0L,
        val uri: String? = "",
        val url: String? = "",
        val account: Account? = null,
        val inReplyToId: Long? = null,
        val inReplyToAccountId: Long? = null,
        val reblogId: Long = 0L,
        val content: String = "",
        val createdAt: String = "",
        val emojis: ArrayList<Emoji>? = ArrayList(),
        val repliesCount: Int = 0,
        val reblogsCount: Int = 0,
        val favouritesCount: Int = 0,
        val isReblogged: Boolean = false,
        val isFavourited: Boolean = false,
        val isSensitive: Boolean = false,
        val spoilerText: String = "",
        val visibility: String = Visibility.Public.value,
        val mediaAttachments: ArrayList<Attachment<*>> = ArrayList(),
        val mentions: ArrayList<Mention> = ArrayList(),
        val tags: ArrayList<Tag> = ArrayList(),
        val application: Application? = null,
        val language: String? = null,
        val pinned: Boolean? = null,
        // Non-standard fields
        val statusIndex: Int = -1,
        val source: String = "undefined") {

    enum class Visibility(val value: String) {
        Public("public"),
        Unlisted("unlisted"),
        Private("private"),
        Direct("direct")
    }
}
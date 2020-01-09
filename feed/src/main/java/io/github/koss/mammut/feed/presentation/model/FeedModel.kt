package io.github.koss.mammut.feed.presentation.model

import com.sys1yagi.mastodon4j.api.entity.Attachment
import io.github.koss.mammut.data.models.Status

sealed class FeedModel(val content: String)

object BrokenTimelineModel: FeedModel(content = "broken_timeline_viewholder")

data class StatusModel(
    val id: Long,
    val name: String,
    val username: String,
    val renderedUsername: CharSequence,
    val renderedContent: CharSequence,
    val createdAt: String,
    val displayAttachments: List<Attachment<*>>,
    val isRetooted: Boolean?,
    val isBoosted: Boolean?,
    val avatar: String,
    val spoilerText: String,
    val isSensitive: Boolean,
    val boostCount: Int,
    val retootCount: Int,
    val status: Status
): FeedModel(content = renderedContent.toString())
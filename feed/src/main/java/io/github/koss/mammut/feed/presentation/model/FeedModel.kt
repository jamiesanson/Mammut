package io.github.koss.mammut.feed.presentation.model

import io.github.koss.mammut.data.models.Status

sealed class FeedModel(val content: String)

object BrokenTimelineModel: FeedModel(content = "broken_timeline_viewholder")

data class StatusModel(
    val status: Status
): FeedModel(content = status.content)
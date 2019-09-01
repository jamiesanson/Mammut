package io.github.koss.mammut.feed.presentation.model

import io.github.koss.mammut.data.models.Status

sealed class FeedModel

object BrokenTimelineModel: FeedModel()

data class StatusModel(
    val status: Status
): FeedModel()
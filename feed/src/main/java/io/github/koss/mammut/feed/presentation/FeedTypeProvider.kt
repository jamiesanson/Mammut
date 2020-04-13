package io.github.koss.mammut.feed.presentation

import io.github.koss.mammut.data.models.domain.FeedType

interface FeedTypeProvider {

    val currentFeedType: FeedType?
}
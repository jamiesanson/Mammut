package io.github.koss.mammut.feature.home.presentation.state

import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.data.models.domain.FeedType

sealed class HomeAction

data class OnRegistrationsLoaded(
        val registrations: List<InstanceRegistration>
) : HomeAction()

data class OnFeedTypeChanged(
        val newFeedType: FeedType
) : HomeAction()

data class OnOffscreenItemCountChanged(
        val newCount: Int
) : HomeAction()
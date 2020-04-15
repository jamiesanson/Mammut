package io.github.koss.mammut.feature.instance.presentation.state

import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.domain.FeedType

data class InstanceState(
        val instanceName: String,
        val accessToken: String,
        val offscreenItemCount: Int = 0,
        val currentUser: Account? = null,
        val allAccounts: Set<Account> = emptySet(),
        val selectedFeedType: FeedType = FeedType.Home
)
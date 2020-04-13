package io.github.koss.mammut.feature.instance2.presentation.state

import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.domain.FeedType

data class InstanceState(
        val instanceName: String,
        val accessToken: String,
        val currentUser: Account? = null,
        val allAccounts: Set<Account> = emptySet(),
        val selectedFeedType: FeedType = FeedType.Home
)
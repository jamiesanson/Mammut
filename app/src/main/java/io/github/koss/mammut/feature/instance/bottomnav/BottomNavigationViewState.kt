package io.github.koss.mammut.feature.instance.bottomnav

import io.github.koss.mammut.data.models.Account

data class BottomNavigationViewState(
        val instanceName: String,
        val currentUser: Account,
        val allAccounts: Set<Account>
)
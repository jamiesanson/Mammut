package io.github.koss.mammut.feature.instance.bottomnav

import io.github.koss.mammut.data.models.Account

data class BottomNavigationViewState(
        val currentUser: Account,
        val otherAccounts: Set<Account>
)
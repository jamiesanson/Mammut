package io.github.koss.mammut.base.navigation

import io.github.koss.mammut.data.models.Account

interface NavigationHub {

    fun pushProfileController(account: Account)
}
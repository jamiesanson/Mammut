package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import io.github.jamiesanson.mammut.data.models.Account

interface TootCallbacks {

    fun onProfileClicked(account: Account)
}
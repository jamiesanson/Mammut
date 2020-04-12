package io.github.koss.mammut.feature.instance.subfeature.navigation

import android.os.Parcelable
import androidx.annotation.IdRes
import io.github.koss.mammut.R
import kotlinx.android.parcel.Parcelize

sealed class Tab(@IdRes val menuItemId: Int): Parcelable {
    @Parcelize
    object Home: Tab(menuItemId = R.id.home)
    @Parcelize
    object Local: Tab(menuItemId = R.id.localTimeline)
    @Parcelize
    object Federated: Tab(menuItemId = R.id.federatedTimeline)
    @Parcelize
    object Notification: Tab(menuItemId = R.id.notifications)
}
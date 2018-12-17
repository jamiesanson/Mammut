package io.github.koss.mammut.feature.instance.subfeature.navigation

import android.os.Parcelable
import androidx.annotation.IdRes
import io.github.koss.mammut.R
import kotlinx.android.parcel.Parcelize

sealed class Tab(@IdRes val menuItemId: Int): Parcelable {
    @Parcelize
    object Home: Tab(menuItemId = R.id.homeDestination)
    @Parcelize
    object Local: Tab(menuItemId = R.id.localTimelineDestination)
    @Parcelize
    object Federated: Tab(menuItemId = R.id.federatedTimelineDestination)
    @Parcelize
    object Profile: Tab(menuItemId = R.id.profileDestination)
}
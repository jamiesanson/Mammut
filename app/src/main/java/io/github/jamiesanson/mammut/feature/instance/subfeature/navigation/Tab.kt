package io.github.jamiesanson.mammut.feature.instance.subfeature.navigation

import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedFragment
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedType
import kotlinx.android.parcel.Parcelize

sealed class Tab(@IdRes val menuItemId: Int, val fragment: Fragment): Parcelable {
    @Parcelize
    object Home: Tab(fragment = FeedFragment.newInstance(FeedType.Home), menuItemId = R.id.homeDestination)
    @Parcelize
    object Local: Tab(fragment = FeedFragment.newInstance(FeedType.Local), menuItemId = R.id.localTimelineDestination)
    @Parcelize
    object Federated: Tab(fragment = FeedFragment.newInstance(FeedType.Federated), menuItemId = R.id.federatedTimelineDestination)
    @Parcelize
    object Notifications: Tab(fragment = Fragment(), menuItemId = R.id.notificationDestination)
}
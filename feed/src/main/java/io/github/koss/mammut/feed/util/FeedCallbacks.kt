package io.github.koss.mammut.feed.util

import android.widget.ImageView
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.Status

interface FeedCallbacks {

    fun onProfileClicked(account: Account)

    fun onPhotoClicked(imageView: ImageView, photoUrl: String)

    fun onTootClicked(status: Status)

    fun onReloadClicked()
}
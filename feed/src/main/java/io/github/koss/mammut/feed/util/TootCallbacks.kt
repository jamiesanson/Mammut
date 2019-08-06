package io.github.koss.mammut.feed.util

import android.widget.ImageView
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.data.models.Account

interface TootCallbacks {

    fun onProfileClicked(account: Account)

    fun onPhotoClicked(imageView: ImageView, photoUrl: String)

    fun onTootClicked(status: Status)
}
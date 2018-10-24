package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.widget.ImageView
import io.github.jamiesanson.mammut.data.models.Account

interface TootCallbacks {

    fun onProfileClicked(account: Account)

    fun onPhotoClicked(imageView: ImageView, photoUrl: String)
}
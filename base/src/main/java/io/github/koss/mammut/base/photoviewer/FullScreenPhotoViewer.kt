package io.github.koss.mammut.base.photoviewer

import android.view.View
import android.widget.ImageView
import com.alexvasilkov.gestures.views.GestureImageView
import com.bumptech.glide.request.RequestOptions

interface FullScreenPhotoViewer {

    fun setPhotoTargetViewBinding(binding: () -> Pair<View, GestureImageView>?)

    fun closeViewerIfVisible(): Boolean

    fun displayPhoto(sourceImageView: ImageView, photoUrl: String, customSourceOptions: RequestOptions? = null)
}
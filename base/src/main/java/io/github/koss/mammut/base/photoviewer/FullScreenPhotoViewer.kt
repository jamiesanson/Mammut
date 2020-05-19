package io.github.koss.mammut.base.photoviewer

import android.view.View
import android.widget.ImageView
import com.alexvasilkov.gestures.views.GestureImageView

interface FullScreenPhotoViewer {

    fun setPhotoTargetViewBinding(binding: () -> Pair<View, GestureImageView>?)

    fun closeViewerIfVisible(): Boolean

    fun displayPhoto(sourceImageView: ImageView, photoUrl: String)
}
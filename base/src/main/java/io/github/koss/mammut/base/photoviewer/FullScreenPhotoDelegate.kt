package io.github.koss.mammut.base.photoviewer

import android.view.View
import android.widget.ImageView
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.alexvasilkov.gestures.views.GestureImageView
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import io.github.koss.mammut.base.util.GlideApp

class FullScreenPhotoDelegate: FullScreenPhotoViewer {

    private var fullScreenImageAnimator: ViewsTransitionAnimator<*>? = null

    private var getPhotoTargetViews: () -> Pair<View, GestureImageView>? = { null }

    override fun setPhotoTargetViewBinding(binding: () -> Pair<View, GestureImageView>?) {
        getPhotoTargetViews = binding
    }

    override fun closeViewerIfVisible(): Boolean {
        if (fullScreenImageAnimator?.isLeaving == false) {
            fullScreenImageAnimator?.exit(true)
            return true
        }

        return false
    }

    override fun displayPhoto(sourceImageView: ImageView, photoUrl: String) {
        val (_, fullScreenGestureImageView) = getPhotoTargetViews() ?: return

        // Setup animator
        fullScreenImageAnimator = GestureTransitions
            .from<Unit>(sourceImageView)
            .into(fullScreenGestureImageView)
            .also {
                it.addPositionUpdateListener { position, isLeaving ->
                    val (fullScreenPhotoLayout, _) = getPhotoTargetViews() ?: return@addPositionUpdateListener
                    fullScreenPhotoLayout.alpha = position
                    val visibility = when {
                        position == 0F && isLeaving -> View.GONE
                        else -> View.VISIBLE
                    }

                    fullScreenPhotoLayout.visibility = visibility
                    fullScreenGestureImageView.visibility = visibility

                    if (position == 0f && isLeaving) {
                        // Invalidate the target to ensure it resizes properly
                        GlideApp.with(sourceImageView)
                            .load(photoUrl)
                            .placeholder(fullScreenGestureImageView.drawable)
                            .transition(withCrossFade())
                            .transform(FitCenter())
                            .into(sourceImageView)
                    }
                }
            }

        // Reset controller state
        fullScreenGestureImageView.controller.resetState()

        // Start the animation
        fullScreenImageAnimator?.enterSingle(true)

        GlideApp.with(sourceImageView)
            .load(photoUrl)
            .placeholder(sourceImageView.drawable)
            .transition(withCrossFade())
            .transform(FitCenter())
            .into(fullScreenGestureImageView)
    }
}
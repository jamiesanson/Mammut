package io.github.koss.mammut.feature.instance.subfeature.feed.media

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.sys1yagi.mastodon4j.api.entity.Attachment
import com.sys1yagi.mastodon4j.api.entity.GifvAttachment
import com.sys1yagi.mastodon4j.api.entity.PhotoAttachment
import com.sys1yagi.mastodon4j.api.entity.VideoAttachment
import io.github.koss.mammut.R
import io.github.koss.mammut.component.GlideApp
import io.github.koss.mammut.extension.inflate
import io.github.koss.mammut.feature.instance.subfeature.feed.TootCallbacks
import kotlinx.android.synthetic.main.media_view_holder.view.*
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.sdk27.coroutines.onClick
import kotlin.math.pow
import kotlin.math.sqrt

class MediaViewHolder(
        parent: ViewGroup,
        private val callbacks: TootCallbacks
): RecyclerView.ViewHolder(parent.inflate(R.layout.media_view_holder)) {

    private var exoPlayer: SimpleExoPlayer? = null

    fun bind(attachment: Attachment<*>, isSensitive: Boolean) {
        processAttachment(attachment)
        setupContentWarning(isSensitive)
    }

    fun unbind() {
        exoPlayer?.release()
    }

    private fun processAttachment(attachment: Attachment<*>) {
        with(itemView) {
            val aspect = attachment.getThumbnailSpec()

            with(ConstraintSet()) {
                clone(constraintLayout)
                setDimensionRatio(tootImageCardView.id, aspect.toString())
                applyTo(constraintLayout)
            }

            tootImageCardView.visibility = View.VISIBLE

            tootImageCardView.doOnLayout {
                // Wait until the next layout pass to ensure the parent is sized correctly
                loadAttachment(attachment)
            }

            tootImageCardView.onClick {
                if (!sensitiveContentFrameLayout.isVisible) {
                    callbacks.onPhotoClicked(tootImageView, attachment.url)
                }
            }
        }

    }

    private fun loadAttachment(attachment: Attachment<*>) {
        when (attachment) {
            is PhotoAttachment -> loadImage(attachment)
            is VideoAttachment -> loadVideo(attachment)
            is GifvAttachment -> loadGifv(attachment)
        }
    }

    private fun loadGifv(gifvAttachment: Attachment<*>) {
        itemView.tootImageView.visibility = View.GONE
        itemView.playerView.visibility = View.VISIBLE
        itemView.playerView.useController = false

        val factory = DefaultDataSourceFactory(itemView.context,
                Util.getUserAgent(itemView.context, "Mammut"))

        val source = ExtractorMediaSource.Factory(factory)
                .createMediaSource(Uri.parse(gifvAttachment.url))

        exoPlayer = ExoPlayerFactory.newSimpleInstance(itemView.context).apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }

        itemView.playerView.player = exoPlayer
        exoPlayer?.prepare(source)
    }

    private fun loadVideo(videoAttachment: Attachment<*>) {
        itemView.tootImageView.visibility = View.GONE
        itemView.playerView.visibility = View.VISIBLE
        itemView.playerView.useController = true

        val factory = DefaultDataSourceFactory(itemView.context,
                Util.getUserAgent(itemView.context, "Mammut"))

        val source = ExtractorMediaSource.Factory(factory)
                .createMediaSource(Uri.parse(videoAttachment.url))

        exoPlayer = ExoPlayerFactory.newSimpleInstance(itemView.context)

        itemView.playerView.player = exoPlayer
        exoPlayer?.prepare(source)
    }

    private fun loadImage(photoAttachment: PhotoAttachment) {
        itemView.tootImageView.visibility = View.VISIBLE
        itemView.playerView.visibility = View.GONE

        // Resolve colors
        val typedValue = TypedValue()
        val theme = itemView.context.theme ?: return
        theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
        @ColorInt val color = typedValue.data

        val requestManager = GlideApp.with(itemView)

        // Load attachment
        requestManager
                .load(photoAttachment.url)
                .thumbnail(
                        requestManager
                                .load(photoAttachment.previewUrl)
                                .thumbnail(
                                        requestManager
                                                .load(ColorDrawable(color))
                                )
                                .transition(DrawableTransitionOptions.withCrossFade())
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.bitmapTransform(FitCenter()))
                .into(itemView.tootImageView)
    }

    private fun setupContentWarning(isSensitive: Boolean) {
        with(itemView) {
            // If not sensitive content, short circuit
            if (!isSensitive) {
                sensitiveContentFrameLayout.isVisible = false
                sensitiveContentToggleButton.isVisible = false
                return
            }

            // Initial conditions
            sensitiveContentFrameLayout.isVisible = true
            sensitiveContentToggleButton.isVisible = true

            fun View.largestDimension(): Float = sqrt(this.width.toFloat().pow(2F) + this.height.toFloat().pow(2F))

            fun toggleContentWarningVisibility() {
                if (sensitiveContentFrameLayout.isVisible) {
                    ViewAnimationUtils.createCircularReveal(
                            sensitiveContentFrameLayout,
                            sensitiveContentFrameLayout.width - sensitiveContentToggleButton.width / 2,
                            sensitiveContentToggleButton.height / 2,
                            sensitiveContentFrameLayout.largestDimension(),
                            0F
                    ).apply {
                        doOnEnd {
                            sensitiveContentFrameLayout.isVisible = false
                        }
                        duration = 250L
                    }.start()

                    sensitiveContentToggleButton.imageResource = R.drawable.ic_visibility_black_24dp
                } else {
                    ViewAnimationUtils.createCircularReveal(
                            sensitiveContentFrameLayout,
                            sensitiveContentFrameLayout.width - sensitiveContentToggleButton.width / 2,
                            sensitiveContentToggleButton.height / 2,
                            0F,
                            sensitiveContentFrameLayout.largestDimension()
                    ).apply {
                        doOnStart {
                            sensitiveContentFrameLayout.isVisible = true
                        }
                        duration = 250L
                    }.start()

                    sensitiveContentToggleButton.imageResource = R.drawable.ic_visibility_off_black_24dp
                }
            }

            sensitiveContentToggleButton.onClick { toggleContentWarningVisibility() }
            sensitiveContentFrameLayout.onClick { toggleContentWarningVisibility() }
        }
    }
}
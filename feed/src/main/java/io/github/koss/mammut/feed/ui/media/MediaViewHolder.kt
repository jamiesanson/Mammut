package io.github.koss.mammut.feed.ui.media

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.material.elevation.ElevationOverlayProvider
import com.sys1yagi.mastodon4j.api.entity.Attachment
import com.sys1yagi.mastodon4j.api.entity.GifvAttachment
import com.sys1yagi.mastodon4j.api.entity.PhotoAttachment
import com.sys1yagi.mastodon4j.api.entity.VideoAttachment
import io.github.koss.mammut.base.anko.dip
import io.github.koss.mammut.base.util.GlideApp
import io.github.koss.mammut.base.util.inflate
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.databinding.MediaViewHolderBinding
import io.github.koss.mammut.feed.util.FeedCallbacks
import kotlin.math.pow
import kotlin.math.sqrt

class MediaViewHolder(
        parent: ViewGroup,
        private val callbacks: FeedCallbacks
): RecyclerView.ViewHolder(parent.inflate(R.layout.media_view_holder)) {

    private var exoPlayer: ExoPlayer? = null

    private val binding = MediaViewHolderBinding.bind(itemView)

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
                clone(binding.constraintLayout)
                setDimensionRatio(binding.tootImageCardView.id, aspect.toString())
                applyTo(binding.constraintLayout)
            }

            loadAttachment(attachment)

            binding.tootImageCardView.visibility = View.VISIBLE

            binding.tootImageCardView.setOnClickListener {
                if (!binding.sensitiveContentFrameLayout.isVisible) {
                    callbacks.onPhotoClicked(binding.tootImageView, attachment.url)
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
        with (binding) {
            tootImageView.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            playerView.useController = false

            val factory = DefaultDataSource.Factory(
                itemView.context,
                DefaultHttpDataSource.Factory().apply {
                    setUserAgent(Util.getUserAgent(itemView.context, "Mammut"))
                }
            )

            val source = ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(gifvAttachment.url))

            exoPlayer = ExoPlayer.Builder(itemView.context).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
            }

            playerView.player = exoPlayer

            exoPlayer?.apply {
                setMediaSource(source)
                prepare()
            }
        }
    }

    private fun loadVideo(videoAttachment: Attachment<*>) {
        with (binding) {
            tootImageView.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            playerView.useController = true

            val factory = DefaultDataSource.Factory(
                itemView.context,
                DefaultHttpDataSource.Factory().apply {
                    setUserAgent(Util.getUserAgent(itemView.context, "Mammut"))
                }
            )

            val source = ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(videoAttachment.url))

            exoPlayer = ExoPlayer.Builder(itemView.context).build()

            playerView.player = exoPlayer

            exoPlayer?.apply {
                setMediaSource(source)
                prepare()
            }
        }
    }

    private fun loadImage(photoAttachment: PhotoAttachment) {
        with (binding) {
            tootImageView.visibility = View.VISIBLE
            playerView.visibility = View.GONE

            // Resolve colors
            @ColorInt val color = ElevationOverlayProvider(itemView.context)
                .compositeOverlayWithThemeSurfaceColorIfNeeded(itemView.context.dip(8f).toFloat())

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
                .into(tootImageView)
        }
    }

    private fun setupContentWarning(isSensitive: Boolean) {
        with(binding) {
            // If not sensitive content, short circuit
            if (!isSensitive) {
                sensitiveContentFrameLayout.isVisible = false
                sensitiveContentToggleButton.isVisible = false
                return
            }


            @ColorInt val color = ElevationOverlayProvider(itemView.context)
                    .compositeOverlayWithThemeSurfaceColorIfNeeded(itemView.context.dip(8).toFloat())

            // Initial conditions
            sensitiveContentFrameLayout.isVisible = true
            sensitiveContentFrameLayout.setBackgroundColor(color)

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

                    sensitiveContentToggleButton.setImageResource(R.drawable.ic_visibility_black_24dp)
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

                    sensitiveContentToggleButton.setImageResource(R.drawable.ic_visibility_off_black_24dp)
                }
            }

            sensitiveContentToggleButton.setOnClickListener { toggleContentWarningVisibility() }
            sensitiveContentFrameLayout.setOnClickListener { toggleContentWarningVisibility() }
        }
    }
}
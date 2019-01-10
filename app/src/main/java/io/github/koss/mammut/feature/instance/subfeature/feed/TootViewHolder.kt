package io.github.koss.mammut.feature.instance.subfeature.feed

import android.animation.Animator
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
import androidx.core.text.HtmlCompat
import androidx.core.view.*
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
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
import io.github.koss.mammut.component.util.Blurrer
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.extension.inflate
import kotlinx.android.synthetic.main.view_holder_feed_item.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.sqrt


class TootViewHolder(parent: ViewGroup) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_feed_item)), CoroutineScope by GlobalScope {

    private var countJob = Job()

    private var currentStatus: Status? = null

    private var exoPlayer: SimpleExoPlayer? = null

    private var isSensitiveScreenVisible = false

    fun bind(status: Status, callbacks: TootCallbacks, requestManager: RequestManager) {
        if (status.id == currentStatus?.id) return

        currentStatus = status

        val submissionTime = ZonedDateTime.parse(status.createdAt)

        with(itemView) {
            onClick {
                callbacks.onTootClicked(status)
            }

            displayNameTextView.text = if (status.account?.displayName?.isEmpty() == true) status.account.acct else status.account?.displayName
            usernameTextView.text = "@${status.account?.userName}"
            contentTextView.text = HtmlCompat.fromHtml(status.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()

            // Configure counting
            countJob.cancel()
            countJob = launch {
                while (true) {
                    withContext(Dispatchers.Main) {
                        val timeSinceSubmission = Duration.between(submissionTime, ZonedDateTime.now())
                        timeTextView.text = timeSinceSubmission.toElapsedTime()
                    }
                    delay(1000)
                }
            }

            // Configure profile click
            status.account?.let { account ->
                profileImageView.onClick {
                    callbacks.onProfileClicked(account)
                }
            }

            // Setup sensitive content screen
            setupContentWarning(isSensitive = status.isSensitive)

            // Resolve colors
            val typedValue = TypedValue()
            val theme = itemView.context.theme ?: return
            theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
            @ColorInt val color = typedValue.data

            requestManager
                    .load(status.account?.avatar)
                    .thumbnail(
                            requestManager
                                    .load(ColorDrawable(color))
                                    .apply(RequestOptions.circleCropTransform())
                    )
                    .transition(withCrossFade())
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileImageView)

            status.mediaAttachments.firstOrNull()?.let { attachment ->
                requestManager
                        .clear(tootImageView)

                val aspect = getThumbnailSpec(attachment)

                with(ConstraintSet()) {
                    clone(constraintLayout)
                    setDimensionRatio(tootImageCardView.id, aspect.toString())
                    applyTo(constraintLayout)
                }

                tootImageCardView.visibility = View.VISIBLE

                tootImageCardView.doOnLayout {
                    // Wait until the next layout pass to ensure the parent is sized correctly
                    loadAttachment(attachment, requestManager)
                }

                tootImageCardView.onClick {
                    if (!isSensitiveScreenVisible) {
                        callbacks.onPhotoClicked(tootImageView, attachment.url)
                    }
                }
            } ?: run {
                with(ConstraintSet()) {
                    clone(constraintLayout)
                    setDimensionRatio(tootImageCardView.id, "0")
                    applyTo(constraintLayout)
                }

                tootImageView.image = null
                tootImageCardView.visibility = View.INVISIBLE
            }
        }

        // Setup spoilers
        setupSpoiler(status.spoilerText)
    }

    private fun loadAttachment(attachment: Attachment<*>, requestManager: RequestManager) {
        when (attachment) {
            is PhotoAttachment -> loadImage(attachment, requestManager)
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

    private fun loadImage(photoAttachment: PhotoAttachment, requestManager: RequestManager) {
        itemView.tootImageView.visibility = View.VISIBLE
        itemView.playerView.visibility = View.GONE

        // Resolve colors
        val typedValue = TypedValue()
        val theme = itemView.context.theme ?: return
        theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
        @ColorInt val color = typedValue.data

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
                                .transition(withCrossFade())
                )
                .transition(withCrossFade())
                .apply(RequestOptions.bitmapTransform(FitCenter()))
                .into(itemView.tootImageView)
    }

    private fun setupContentWarning(isSensitive: Boolean) {
        with(itemView) {
            // If not sensitive content, short circuit
            if (!isSensitive) {
                isSensitiveScreenVisible = false
                sensitiveContentFrameLayout.isVisible = false
                sensitiveContentToggleButton.isVisible = false
                return
            }

            // Initial conditions
            sensitiveContentFrameLayout.isVisible = true
            sensitiveContentToggleButton.isVisible = true
            isSensitiveScreenVisible = true

            fun View.largestDimension(): Float = sqrt(this.width.toFloat().pow(2F) + this.height.toFloat().pow(2F))

            fun toggleContentWarningVisibility() {
                if (isSensitiveScreenVisible) {
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

                    TransitionManager.beginDelayedTransition(tootImageCardView)
                    sensitiveContentToggleButton.imageResource = R.drawable.ic_visibility_black_24dp
                    isSensitiveScreenVisible = false
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

                    TransitionManager.beginDelayedTransition(tootImageCardView)
                    sensitiveContentToggleButton.imageResource = R.drawable.ic_visibility_off_black_24dp
                    isSensitiveScreenVisible = true
                }
            }

            sensitiveContentToggleButton.onClick { toggleContentWarningVisibility() }
            sensitiveContentFrameLayout.onClick { toggleContentWarningVisibility() }
        }
    }

    private fun setupSpoiler(spoilerText: String) {
        itemView.spoilerTextView.text = spoilerText
        if (spoilerText.isNotEmpty()) {
            itemView.doOnPreDraw {
                itemView.blurParentLayout.isVisible = true
            }
            itemView.doOnNextLayout {
                Glide.with(itemView)
                        .load(Blurrer.blurView(itemView, 25F))
                        .transition(withCrossFade())
                        .into(itemView.blurLayout)
            }

            itemView.blurParentLayout.onClick {
                itemView.blurParentLayout.animate()
                        .alpha(0F)
                        .setDuration(300L)
                        .setListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {}
                            override fun onAnimationCancel(animation: Animator?) {}
                            override fun onAnimationStart(animation: Animator?) {}

                            override fun onAnimationEnd(animation: Animator?) {
                                itemView.blurParentLayout.isVisible = false
                            }
                        })

                        .start()
            }

            itemView.blurParentLayout.alpha = 1F
        } else {
            itemView.blurParentLayout.isVisible = false
        }
    }

    /**
     * Function for inspecting an attachment for metadata and retrieving an approximate
     * width and height. Will assume 4:3 width:height ratio if nothing found
     */
    private fun getThumbnailSpec(attachment: Attachment<*>): Float {
        val bestGuess = 400F / 300F
        return when (attachment) {
            is PhotoAttachment -> attachment.metadata?.original?.run {
                when {
                    aspect != 0F -> aspect
                    width != 0 && height != 0 -> width.toFloat() / height.toFloat()
                    else -> bestGuess
                }
            } ?: bestGuess
            is VideoAttachment -> attachment.metadata?.original?.run {
                if (width != 0 && height != 0) width.toFloat() / height.toFloat() else bestGuess
            } ?: bestGuess
            is GifvAttachment -> attachment.metadata?.original?.run {
                if (width != 0 && height != 0) width.toFloat() / height.toFloat() else bestGuess
            } ?: bestGuess
            else -> throw IllegalArgumentException("Unknown attachment type")
        }
    }

    private fun Duration.toElapsedTime(): String =
            when {
                this > Duration.of(7, ChronoUnit.DAYS) -> "${toDays() / 7} weeks ago"
                this > Duration.of(1, ChronoUnit.DAYS) -> "${toDays()} days ago"
                this > Duration.of(1, ChronoUnit.HOURS) -> "${toHours()} hours ago"
                this > Duration.of(1, ChronoUnit.MINUTES) -> "${toMinutes()} mins ago"
                this > Duration.of(1, ChronoUnit.SECONDS) -> "${toMillis() / 1000} secs ago"
                else -> "Just now"
            }


    fun clear() {
        with(itemView) {
            displayNameTextView.text = null
            usernameTextView.text = null
            contentTextView.text = null
            timeTextView.text = null
            profileImageView.setOnClickListener(null)
            GlideApp.with(itemView)
                    .clear(profileImageView)
            GlideApp.with(itemView)
                    .clear(tootImageView)
            tootImageView.visibility = View.GONE
        }
        countJob.cancel()
        currentStatus = null
    }

    fun recycle() {
        exoPlayer?.release()
    }
}
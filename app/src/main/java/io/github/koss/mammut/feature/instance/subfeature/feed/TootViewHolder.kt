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
import androidx.core.view.*
import androidx.lifecycle.ViewModelProvider
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
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
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.extension.inflate
import kotlinx.android.synthetic.main.view_holder_feed_item.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class TootViewHolder(
        parent: ViewGroup,
        viewModelProvider: ViewModelProvider,
        private val requestManager: RequestManager,
        private val callbacks: TootCallbacks
) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_feed_item)), CoroutineScope by GlobalScope {

    private var exoPlayer: SimpleExoPlayer? = null

    private var isSensitiveScreenVisible = false
    private var isContentVisible = false

    private var viewModel: TootViewModel = viewModelProvider.get(UUID.randomUUID().toString(), TootViewModel::class.java)

    init {
        // Set up viewModel observations
        viewModel.statusViewState.observeForever(::onViewStateChanged)
        viewModel.timeSince.observeForever(itemView.timeTextView::setText)

        // Set up click listeners
        itemView.onClick {
            viewModel.currentStatus?.let(callbacks::onTootClicked)
        }

        itemView.profileImageView.onClick {
            viewModel.currentStatus?.account?.let(callbacks::onProfileClicked)
        }
    }

    fun bind(status: Status) {
        viewModel.bind(status)
    }

    private fun onViewStateChanged(viewState: TootViewState?) {
        with(itemView) {
            viewState?.name?.let(displayNameTextView::setText)
            viewState?.username?.let(usernameTextView::setText)
            viewState?.content?.let(contentTextView::setText)
        }

        viewState?.displayAttachment.let(::processAttachment)

        // Setup sensitive content screen
        setupContentWarning(isSensitive = viewModel.currentStatus?.isSensitive ?: false)

        @ColorInt val color = itemView.colorAttr(R.attr.colorPrimaryLight)

        requestManager
                .load(viewModel.currentStatus?.account?.avatar)
                .thumbnail(
                        requestManager
                                .load(ColorDrawable(color))
                                .apply(RequestOptions.circleCropTransform())
                )
                .transition(withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(itemView.profileImageView)


        // Setup spoilers
        setupSpoiler(viewModel.currentStatus?.spoilerText ?: "")
    }

    private fun processAttachment(att: Attachment<*>?) {
        with(itemView) {
            att?.let { attachment ->
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
                    loadAttachment(attachment)
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
    }

    private fun loadAttachment(attachment: Attachment<*>) {
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

    private fun setupSpoiler(spoilerText: String) = with(itemView) {
        contentWarningTextView.text = spoilerText
        contentWarningTextView.isVisible = spoilerText.isNotEmpty()
        contentWarningVisibilityButton.isVisible = spoilerText.isNotEmpty()
        isContentVisible = !spoilerText.isNotEmpty()

        fun renderContentVisibility(transition: Boolean) {
            if (transition) {
                TransitionManager.beginDelayedTransition(itemView.parent as ViewGroup)
            }

            // Change visibility
            contentTextView.isVisible = isContentVisible
            val hideImageView = viewModel.statusViewState.value?.displayAttachment != null
            if (hideImageView) tootImageCardView.isVisible = isContentVisible

            // Change button icon
            contentWarningVisibilityButton.imageResource =
                    if (isContentVisible) R.drawable.ic_visibility_black_24dp else R.drawable.ic_visibility_off_black_24dp
        }

        if (spoilerText.isNotEmpty()) {
            contentWarningVisibilityButton.onClick {
                isContentVisible = !isContentVisible
                renderContentVisibility(transition = true)
            }
        }

        renderContentVisibility(false)
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

    fun recycle() {
        exoPlayer?.release()
        viewModel.onCleared()
    }
}
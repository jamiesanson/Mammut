package io.github.koss.mammut.feature.instance.subfeature.feed

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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import arrow.optics.typeclasses.At
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.github.ajalt.flexadapter.FlexAdapter
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
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.extension.inflate
import io.github.koss.mammut.extension.observe
import io.github.koss.mammut.feature.instance.subfeature.feed.media.MediaAdapter
import io.github.koss.mammut.feature.instance.subfeature.feed.media.getThumbnailSpec
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
        private val callbacks: TootCallbacks
) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_feed_item)), CoroutineScope by GlobalScope {

    private var exoPlayer: SimpleExoPlayer? = null

    private var viewModel: TootViewModel = viewModelProvider.get(UUID.randomUUID().toString(), TootViewModel::class.java)

    init {
        // Set up viewModel observations
        viewModel.statusViewState.observe(itemView.context as LifecycleOwner, ::onViewStateChanged)
        viewModel.timeSince.observe(itemView.context as LifecycleOwner, itemView.timeTextView::setText)

        // Set up click listeners
        with (itemView) {
            onClick {
                callbacks.onTootClicked(viewModel.currentStatus)
            }
            profileImageView.onClick {
                viewModel.currentStatus.account?.let(callbacks::onProfileClicked)
            }
            boostButton.onClick {
                viewModel.onBoostClicked()
            }
            retootButton.onClick {
                viewModel.onRetootClicked()
            }
        }
    }

    fun bind(status: Status) {
        viewModel.bind(status)
    }

    private fun onViewStateChanged(viewState: TootViewState?) {
        viewState ?: return

        with(itemView) {
            viewState.name.let(displayNameTextView::setText)
            viewState.username.let(usernameTextView::setText)
            viewState.content.let(contentTextView::setText)
        }

        when {
            viewState.displayAttachments.isEmpty() ->
                itemView.attachmentsRecyclerView.isVisible = false
            viewState.displayAttachments.isNotEmpty() ->
                renderAttachments(viewState.displayAttachments, viewState.isSensitive)
        }

        with (itemView.boostButton) {
            when (viewState.isBoosted) {
                true -> {
                    isEnabled = true
                    textColor = colorAttr(R.attr.colorAccent)
                }
                false -> {
                    isEnabled = true
                    textColor = colorAttr(R.attr.colorControlNormalTransparent)
                }
                null -> isEnabled = false
            }

            text = if (viewState.boostCount > 0) viewState.boostCount.toString() else ""
        }

        with (itemView.retootButton) {
            when (viewState.isRetooted) {
                true -> {
                    isEnabled = true
                    textColor = colorAttr(R.attr.colorAccent)
                }
                false -> {
                    isEnabled = true
                    textColor = colorAttr(R.attr.colorControlNormalTransparent)
                }
                null -> isEnabled = false
            }

            text = if (viewState.retootCount > 0) viewState.retootCount.toString() else ""
        }

        @ColorInt val color = itemView.colorAttr(R.attr.colorPrimaryLight)

        val requestManager = GlideApp.with(itemView)

        requestManager
                .load(viewState.avatar)
                .thumbnail(
                        requestManager
                                .load(ColorDrawable(color))
                                .apply(RequestOptions.circleCropTransform())
                )
                .transition(withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(itemView.profileImageView)

        // Setup spoilers
        setupSpoiler(viewState.spoilerText)
    }

    private fun renderAttachments(attachments: List<Attachment<*>>, isSensitive: Boolean) {
        with (itemView) {
            with(ConstraintSet()) {
                clone(recyclerViewConstraintLayout)
                setDimensionRatio(attachmentsRecyclerView.id, (attachments.first().getThumbnailSpec() * 1.2F).toString())
                applyTo(recyclerViewConstraintLayout)
            }

            when {
                attachmentsRecyclerView.adapter == null -> {
                    attachmentsRecyclerView.adapter = MediaAdapter(callbacks)
                    attachmentsRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                    LinearSnapHelper().attachToRecyclerView(attachmentsRecyclerView)
                }
            }

            (attachmentsRecyclerView.adapter as MediaAdapter).apply {
                contentIsSensitive = isSensitive
                submitList(attachments)
            }
        }
    }

    private fun setupSpoiler(spoilerText: String) = with(itemView) {
        contentWarningTextView.text = spoilerText
        contentWarningTextView.isVisible = spoilerText.isNotEmpty()
        contentWarningVisibilityButton.isVisible = spoilerText.isNotEmpty()
        viewModel.isContentVisible = spoilerText.isEmpty()

        fun renderContentVisibility(transition: Boolean) {
            if (transition) {
                TransitionManager.beginDelayedTransition(itemView.parent as ViewGroup, AutoTransition().apply { duration = 200L })
            }

            // Change visibility
            contentTextView.isVisible = viewModel.isContentVisible
            val hideImageView = viewModel.statusViewState.value?.displayAttachments?.isNotEmpty() == true
            if (hideImageView) attachmentsRecyclerView.isVisible = viewModel.isContentVisible

            // Change button icon
            contentWarningVisibilityButton.imageResource =
                    if (viewModel.isContentVisible) R.drawable.ic_visibility_black_24dp else R.drawable.ic_visibility_off_black_24dp
        }

        if (spoilerText.isNotEmpty()) {
            contentWarningVisibilityButton.onClick {
                viewModel.isContentVisible = !viewModel.isContentVisible
                renderContentVisibility(transition = true)
            }
        }

        renderContentVisibility(false)
    }

    fun recycle() {
        exoPlayer?.release()
        viewModel.onCleared()
    }
}
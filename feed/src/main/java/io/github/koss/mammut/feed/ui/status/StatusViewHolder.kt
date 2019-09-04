package io.github.koss.mammut.feed.ui.status

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.SimpleExoPlayer
import com.sys1yagi.mastodon4j.api.entity.Attachment
import io.github.koss.mammut.base.util.GlideApp
import io.github.koss.mammut.base.util.inflate
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.ui.list.FeedItemViewHolder
import io.github.koss.mammut.feed.ui.media.MediaAdapter
import io.github.koss.mammut.feed.ui.media.getThumbnailSpec
import io.github.koss.mammut.feed.util.FeedCallbacks
import kotlinx.android.synthetic.main.view_holder_feed_item.*
import kotlinx.android.synthetic.main.view_holder_feed_item.attachmentsRecyclerView
import kotlinx.android.synthetic.main.view_holder_feed_item.contentTextView
import kotlinx.android.synthetic.main.view_holder_feed_item.contentWarningTextView
import kotlinx.android.synthetic.main.view_holder_feed_item.contentWarningVisibilityButton
import kotlinx.android.synthetic.main.view_holder_feed_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.textColor

class StatusViewHolder(
    parent: ViewGroup,
    private val viewModelProvider: ViewModelProvider,
    private val callbacks: FeedCallbacks
) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_feed_item)), CoroutineScope by GlobalScope {

    private var exoPlayer: SimpleExoPlayer? = null
    private var viewModel: StatusViewModel? = null
    private var job: Job? = null

    init {
        itemView.onClick {
            viewModel?.currentStatus?.let(callbacks::onTootClicked)
        }
        profileImageView.onClick {
            viewModel?.currentStatus?.account?.let(callbacks::onProfileClicked)
        }
        boostButton.onClick {
            viewModel?.onBoostClicked()
        }
        retootButton.onClick {
            viewModel?.onRetootClicked()
        }
    }

    fun bind(status: Status) {
        viewModel = viewModelProvider.get(status.id.toString(), StatusViewModel::class.java)

        // Immediately resize the cell
        setHeightImmediately(status)

        // Submit to the ViewModel to get additional updates
        viewModel?.submitStatus(status)

        // If we're rebinding and we already have a state, use it
        @Suppress("EXPERIMENTAL_API_USAGE")
        viewModel!!.viewState.valueOrNull?.let(::renderState)

        job?.cancel()
        job = launch {
            launch {
                for (state in viewModel!!.viewState.openSubscription()) {
                    withContext(Dispatchers.Main) {
                        renderState(state)
                    }
                }
            }

            launch {
                for (time in viewModel!!.timeSince.openSubscription()) {
                    withContext(Dispatchers.Main) {
                        timeTextView.text = time
                    }
                }
            }
        }
    }

    fun dettach() {
        job?.cancel()
    }

    fun recycle() {
        exoPlayer?.release()
    }

    private fun setHeightImmediately(status: Status) {
        if (status.mediaAttachments.isEmpty()) return

        with(ConstraintSet()) {
            clone(recyclerViewConstraintLayout)
            setDimensionRatio(attachmentsRecyclerView.id, (status.mediaAttachments.first().getThumbnailSpec() * 1.2F).toString())
            applyTo(recyclerViewConstraintLayout)
        }
    }

    private fun renderState(viewState: StatusViewState) {
        viewState.name.let(displayNameTextView::setText)
        viewState.username.let(usernameTextView::setText)
        viewState.content.let(contentTextView::setText)

        when {
            viewState.displayAttachments.isEmpty() ->
                attachmentsRecyclerView.isVisible = false
            viewState.displayAttachments.isNotEmpty() ->
                renderAttachments(viewState.displayAttachments, viewState.isSensitive)
        }

        with (boostButton) {
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

        with (retootButton) {
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

        if ((itemView.context as AppCompatActivity).isDestroyed) return

        val requestManager = GlideApp.with(itemView)

        requestManager
            .load(viewState.avatar)
            .thumbnail(
                requestManager
                    .load(ColorDrawable(color))
                    .apply(RequestOptions.circleCropTransform())
            )
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions.circleCropTransform())
            .into(profileImageView)

        // Setup spoilers
        setupSpoiler(viewState)
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

    private fun setupSpoiler(state: StatusViewState) = with(itemView) {
        contentWarningTextView.text = state.spoilerText
        contentWarningTextView.isVisible = state.spoilerText.isNotEmpty()
        contentWarningVisibilityButton.isVisible = state.spoilerText.isNotEmpty()
        viewModel?.isContentVisible = state.spoilerText.isEmpty()

        fun renderContentVisibility(transition: Boolean) {
            if (transition) {
                TransitionManager.beginDelayedTransition(itemView.parent as ViewGroup, AutoTransition().apply { duration = 200L })
            }

            // Change visibility
            contentTextView.isVisible = viewModel?.isContentVisible ?: true
            val hideImageView = state.displayAttachments.isNotEmpty()
            if (hideImageView) attachmentsRecyclerView.isVisible = viewModel?.isContentVisible ?: true

            // Change button icon
            contentWarningVisibilityButton.imageResource =
                if (viewModel?.isContentVisible == true) R.drawable.ic_visibility_black_24dp else R.drawable.ic_visibility_off_black_24dp
        }

        if (state.spoilerText.isNotEmpty()) {
            contentWarningVisibilityButton.onClick {
                viewModel?.let {
                    it.isContentVisible = !it.isContentVisible
                }
                renderContentVisibility(transition = true)
            }
        }

        renderContentVisibility(false)
    }
}

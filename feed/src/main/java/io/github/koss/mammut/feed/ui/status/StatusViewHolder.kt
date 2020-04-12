package io.github.koss.mammut.feed.ui.status

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.PrecomputedTextCompat
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
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.presentation.model.StatusModel
import io.github.koss.mammut.feed.ui.list.FeedItemViewHolder
import io.github.koss.mammut.feed.ui.media.MediaAdapter
import io.github.koss.mammut.feed.ui.media.processSpec
import io.github.koss.mammut.feed.ui.view.TriStateButton
import io.github.koss.mammut.feed.util.FeedCallbacks
import kotlinx.android.synthetic.main.view_holder_feed_item.*
import kotlinx.android.synthetic.main.view_holder_feed_item.attachmentsRecyclerView
import kotlinx.android.synthetic.main.view_holder_feed_item.contentTextView
import kotlinx.android.synthetic.main.view_holder_feed_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.sdk27.coroutines.onClick

@ExperimentalCoroutinesApi
class StatusViewHolder(
    parent: ViewGroup,
    private val viewModelProvider: ViewModelProvider,
    private val callbacks: FeedCallbacks
) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_feed_item)), CoroutineScope by GlobalScope {

    private var exoPlayer: SimpleExoPlayer? = null
    private lateinit var viewModel: StatusViewModel
    private var job: Job? = null

    init {
        itemView.onClick {
            viewModel.currentStatus.status.let(callbacks::onTootClicked)
        }
        profileImageView.onClick {
            viewModel.currentStatus.status.account?.let(callbacks::onProfileClicked)
        }
        boostButton.onClick {
            viewModel.onBoostClicked()
        }
        retootButton.onClick {
            viewModel.onRetootClicked()
        }

        replyButton.updateState(TriStateButton.State.INACTIVE)
    }

    fun bind(status: StatusModel) {
        viewModel = viewModelProvider.get(status.id.toString(), StatusViewModel::class.java)
        renderState(status)

        job?.cancel()
        viewModel.onNewModel(status)

        job = launch(Dispatchers.Main) {
            for (item in viewModel.statusOverrides.openSubscription()) {
                applyOverrides(item)
            }
        }
    }

    fun detach() {
        job?.cancel()
    }

    fun recycle() {
        exoPlayer?.release()
    }

    private fun applyOverrides(statusOverrides: StatusOverrides) {
        timeTextView.text = statusOverrides.submissionTime
        boostButton.updateState(statusOverrides.isBoosted.toButtonState())
        retootButton.updateState(statusOverrides.isRetooted.toButtonState())
    }

    private fun renderState(viewState: StatusModel) {
        viewState.name.let(displayNameTextView::setText)
        viewState.username.let(usernameTextView::setText)

        val usernameParams = usernameTextView.textMetricsParamsCompat
        usernameTextView.text = viewState.username
        usernameTextView.setTextFuture(PrecomputedTextCompat.getTextFuture(viewState.renderedUsername, usernameParams, null))

        val params = contentTextView.textMetricsParamsCompat
        contentTextView.text = viewState.content
        contentTextView.setTextFuture(PrecomputedTextCompat.getTextFuture(viewState.renderedContent, params, null))

        when {
            viewState.displayAttachments.isEmpty() ->
                attachmentsRecyclerView.isVisible = false
            viewState.displayAttachments.isNotEmpty() ->
                renderAttachments(viewState.displayAttachments, viewState.isSensitive)
        }

        with(boostButton) {
            boostButton.updateState(viewState.isBoosted.toButtonState())
            text = if (viewState.boostCount > 0) viewState.boostCount.toString() else ""
        }

        with(retootButton) {
            retootButton.updateState(viewState.isRetooted.toButtonState())
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
        with(itemView) {
            with(ConstraintSet()) {
                clone(recyclerViewConstraintLayout)
                @Suppress("SENSELESS_COMPARISON") // lmao
                if (attachments.first() == null) return
                setDimensionRatio(attachmentsRecyclerView.id, (processSpec(attachments.first()) * 1.2F).toString())
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

    private fun setupSpoiler(state: StatusModel) = with(itemView) {
        contentWarningTextView.text = state.spoilerText
        contentWarningTextView.isVisible = state.spoilerText.isNotEmpty()
        contentWarningVisibilityButton.isVisible = state.spoilerText.isNotEmpty()
        viewModel.isContentVisible = state.spoilerText.isEmpty()

        fun renderContentVisibility(transition: Boolean) {
            if (transition) {
                TransitionManager.beginDelayedTransition(itemView.parent as ViewGroup, AutoTransition().apply { duration = 200L })
            }

            // Change visibility
            contentTextView.isVisible = viewModel.isContentVisible
            val hideImageView = state.displayAttachments.isNotEmpty()
            if (hideImageView) attachmentsRecyclerView.isVisible = viewModel.isContentVisible

            // Change button icon
            contentWarningVisibilityButton.imageResource =
                if (viewModel.isContentVisible) R.drawable.ic_visibility_black_24dp else R.drawable.ic_visibility_off_black_24dp
        }

        if (state.spoilerText.isNotEmpty()) {
            contentWarningVisibilityButton.onClick {
                viewModel.let {
                    it.isContentVisible = !it.isContentVisible
                }
                renderContentVisibility(transition = true)
            }
        }

        renderContentVisibility(false)
    }

    private fun Boolean?.toButtonState(): TriStateButton.State = when (this) {
        true -> TriStateButton.State.ACTIVE
        false -> TriStateButton.State.INACTIVE
        null -> TriStateButton.State.PENDING
    }
}

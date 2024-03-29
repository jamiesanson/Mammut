package io.github.koss.mammut.feed.ui.status

import android.graphics.drawable.ColorDrawable
import android.text.method.LinkMovementMethod
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
import com.google.android.material.elevation.ElevationOverlayProvider
import com.sys1yagi.mastodon4j.api.entity.Attachment
import io.github.koss.mammut.base.anko.dip
import io.github.koss.mammut.base.util.GlideApp
import io.github.koss.mammut.base.util.inflate
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.databinding.ViewHolderFeedItemBinding
import io.github.koss.mammut.feed.presentation.model.StatusModel
import io.github.koss.mammut.feed.ui.list.FeedItemViewHolder
import io.github.koss.mammut.feed.ui.media.MediaAdapter
import io.github.koss.mammut.feed.ui.media.processSpec
import io.github.koss.mammut.feed.ui.view.TriStateButton
import io.github.koss.mammut.feed.util.FeedCallbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class StatusViewHolder(
    parent: ViewGroup,
    private val viewModelProvider: ViewModelProvider,
    private val callbacks: FeedCallbacks
) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_feed_item)), CoroutineScope by GlobalScope {

    private lateinit var viewModel: StatusViewModel
    private var job: Job? = null

    private val binding by lazy { ViewHolderFeedItemBinding.bind(itemView) }

    init {
        with (binding) {
            root.setOnClickListener {
                viewModel.currentStatus.status.let(callbacks::onTootClicked)
            }

            profileImageView.setOnClickListener {
                viewModel.currentStatus.status.account?.let(callbacks::onProfileClicked)
            }
            boostButton.setOnClickListener {
                viewModel.onBoostClicked()
            }
            retootButton.setOnClickListener {
                viewModel.onRetootClicked()
            }

            contentTextView.movementMethod = LinkMovementMethod()

            replyButton.updateState(TriStateButton.State.INACTIVE)
        }
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

    private fun applyOverrides(statusOverrides: StatusOverrides) {
        with (binding) {
            timeTextView.text = statusOverrides.submissionTime
            boostButton.updateState(statusOverrides.isBoosted.toButtonState())
            retootButton.updateState(statusOverrides.isRetooted.toButtonState())
        }
    }

    private fun renderState(viewState: StatusModel) = with (binding) {
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
            else ->
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

        @ColorInt val color = ElevationOverlayProvider(itemView.context)
                .compositeOverlayWithThemeSurfaceColorIfNeeded(itemView.context.dip(8f).toFloat())

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
        with(binding) {
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
                    attachmentsRecyclerView.layoutManager = LinearLayoutManager(root.context, RecyclerView.HORIZONTAL, false)
                    LinearSnapHelper().attachToRecyclerView(attachmentsRecyclerView)
                }
            }

            (attachmentsRecyclerView.adapter as MediaAdapter).apply {
                contentIsSensitive = isSensitive
                submitList(attachments)
            }
        }
    }

    private fun setupSpoiler(state: StatusModel) = with(binding) {
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
            contentWarningVisibilityButton.setImageResource(if (viewModel.isContentVisible) R.drawable.ic_visibility_black_24dp else R.drawable.ic_visibility_off_black_24dp)
        }

        if (state.spoilerText.isNotEmpty()) {
            contentWarningVisibilityButton.setOnClickListener {
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

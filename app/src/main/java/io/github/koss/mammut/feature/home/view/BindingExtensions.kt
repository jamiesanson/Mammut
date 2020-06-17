package io.github.koss.mammut.feature.home.view

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import io.github.koss.mammut.R
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.databinding.HomeFragmentBinding
import org.jetbrains.anko.colorAttr

fun HomeFragmentBinding.setupChooser(
        selectedFeedType: FeedType,
        onTypeChanged: (FeedType) -> Unit
) {

    with(homeFeedTypeCell) {
        iconImageView.setImageResource(R.drawable.ic_home_black_24dp)
        titleTextView.setText(R.string.home_feed_title)
        subtitleTextView.setText(R.string.home_feed_subtitle)
        selectedImageView.isVisible = selectedFeedType == FeedType.Home
        root.setOnClickListener {
            onTypeChanged(FeedType.Home)
            closeChooser()
        }
    }

    with(localFeedTypeCell) {
        iconImageView.setImageResource(R.drawable.ic_people_black_24dp)
        titleTextView.setText(R.string.local_feed_title)
        subtitleTextView.setText(R.string.local_feed_subtitle)
        selectedImageView.isVisible = selectedFeedType == FeedType.Local
        root.setOnClickListener {
            onTypeChanged(FeedType.Local)
            closeChooser()
        }
    }

    with(federatedFeedTypeCell) {
        iconImageView.setImageResource(R.drawable.ic_public_black_24dp)
        titleTextView.setText(R.string.federated_feed_title)
        subtitleTextView.setText(R.string.federated_feed_subtitle)
        selectedImageView.isVisible = selectedFeedType == FeedType.Federated
        root.setOnClickListener {
            onTypeChanged(FeedType.Federated)
            closeChooser()
        }
    }
}

fun HomeFragmentBinding.openChooser() {
    val transform = MaterialContainerTransform().apply {
        startView = feedTypeButton
        endView = feedChooserCard

        setPathMotion(MaterialArcMotion())

        scrimColor = root.context.colorAttr(R.attr.colorControlNormalTransparent)

        addListener(object: Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition) {

                feedTypeDim.visibility = View.VISIBLE
            }

            override fun onTransitionResume(transition: Transition) {}

            override fun onTransitionPause(transition: Transition) {}

            override fun onTransitionCancel(transition: Transition) {}

            override fun onTransitionStart(transition: Transition) {}
        })
    }

    TransitionManager.beginDelayedTransition(root as ViewGroup, transform)
    feedTypeButton.visibility = View.GONE
    feedChooserCard.visibility = View.VISIBLE
}

fun HomeFragmentBinding.closeChooser() {
    val transform = MaterialContainerTransform().apply {
        endView = feedTypeButton
        startView = feedChooserCard

        setPathMotion(MaterialArcMotion())

        scrimColor = root.context.colorAttr(R.attr.colorControlNormalTransparent)
    }

    TransitionManager.beginDelayedTransition(root as ViewGroup, transform)
    feedTypeButton.visibility = View.VISIBLE
    feedChooserCard.visibility = View.GONE
    feedTypeDim.visibility = View.GONE
}

fun HomeFragmentBinding.bindFeedTypeButton(feedType: FeedType) {
    with(feedTypeButton) {
        when (feedType) {
            FeedType.Home -> {
                setIconResource(R.drawable.ic_home_black_24dp)
                setText(R.string.home_feed_title)
            }
            FeedType.Local -> {
                setIconResource(R.drawable.ic_people_black_24dp)
                setText(R.string.local_feed_title)
            }
            FeedType.Federated -> {
                setIconResource(R.drawable.ic_public_black_24dp)
                setText(R.string.federated_feed_title)
            }
        }
    }
}
package io.github.koss.mammut.feature.instance.view

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.core.transition.doOnEnd
import androidx.core.view.isVisible
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import io.github.koss.mammut.R
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.databinding.InstanceFragmentTwoBinding
import org.jetbrains.anko.colorAttr

fun InstanceFragmentTwoBinding.setupChooser(
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

fun InstanceFragmentTwoBinding.openChooser() {
    val transform = MaterialContainerTransform(root.context).apply {
        startView = feedTypeButton
        endView = feedChooserCard

        pathMotion = MaterialArcMotion()

        scrimColor = root.context.colorAttr(R.attr.colorControlNormalTransparent)

        doOnEnd {
            feedTypeDim.visibility = View.VISIBLE
        }
    }

    TransitionManager.beginDelayedTransition(root as ViewGroup, transform)
    feedTypeButton.visibility = View.GONE
    feedChooserCard.visibility = View.VISIBLE
}

fun InstanceFragmentTwoBinding.closeChooser() {
    val transform = MaterialContainerTransform(root.context).apply {
        endView = feedTypeButton
        startView = feedChooserCard

        pathMotion = MaterialArcMotion()

        scrimColor = root.context.colorAttr(R.attr.colorControlNormalTransparent)
    }

    TransitionManager.beginDelayedTransition(root as ViewGroup, transform)
    feedTypeButton.visibility = View.VISIBLE
    feedChooserCard.visibility = View.GONE
    feedTypeDim.visibility = View.GONE
}

fun InstanceFragmentTwoBinding.bindFeedTypeButton(feedType: FeedType) {
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
package io.github.koss.mammut.feed.ui.broken

import android.view.ViewGroup
import io.github.koss.mammut.base.util.inflate
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.databinding.ViewHolderBrokenTimelineBinding
import io.github.koss.mammut.feed.ui.list.FeedItemViewHolder
import io.github.koss.mammut.feed.util.FeedCallbacks

class BrokenTimelineViewHolder(parent: ViewGroup) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_broken_timeline)) {

    private val binding = ViewHolderBrokenTimelineBinding.bind(itemView)

    fun bind(callbacks: FeedCallbacks) {
        binding.findMyOwnWayButton.setOnClickListener {
            callbacks.onReloadClicked()
        }
    }
}
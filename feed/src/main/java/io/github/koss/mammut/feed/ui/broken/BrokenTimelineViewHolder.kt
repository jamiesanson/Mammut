package io.github.koss.mammut.feed.ui.broken

import android.view.ViewGroup
import io.github.koss.mammut.base.util.inflate
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.ui.list.FeedItemViewHolder
import io.github.koss.mammut.feed.util.FeedCallbacks
import kotlinx.android.synthetic.main.view_holder_broken_timeline.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class BrokenTimelineViewHolder(parent: ViewGroup) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_broken_timeline)) {

    fun bind(callbacks: FeedCallbacks) {
        itemView.findMyOwnWayButton.onClick {
            callbacks.onReloadClicked()
        }
    }
}
package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.view.ViewGroup
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.extension.inflate
import kotlinx.android.synthetic.main.view_holder_broken_timeline.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class BrokenTimelineViewHolder(parent: ViewGroup) : FeedItemViewHolder(parent.inflate(R.layout.view_holder_broken_timeline)) {

    fun bind(onClick: () -> Unit) {
        itemView.findMyOwnWayButton.onClick {
            onClick()
        }
    }
}
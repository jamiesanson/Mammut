package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.extension.inflate
import kotlinx.android.synthetic.main.view_holder_feed_item.view.*

class FeedViewHolder(parent: ViewGroup): RecyclerView.ViewHolder(parent.inflate(R.layout.view_holder_feed_item)) {
    fun bind(number: Status) {
        itemView.itemCountTextView.text = "${number.account?.displayName} - ${number.createdAt}"
    }

    fun clear() {
        itemView.itemCountTextView.text = ""
    }
}
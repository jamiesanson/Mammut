package io.github.koss.mammut.feature.instance.subfeature.feed.media

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sys1yagi.mastodon4j.api.entity.Attachment
import io.github.koss.mammut.feature.instance.subfeature.feed.TootCallbacks

class MediaAdapter(
        private val callbacks: TootCallbacks
): ListAdapter<Attachment<*>, MediaViewHolder>(DIFF_CALLBACK) {

    var contentIsSensitive: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder =
            MediaViewHolder(parent, callbacks)

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position), contentIsSensitive)
    }

    override fun onViewRecycled(holder: MediaViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    companion object {
        private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<Attachment<*>>() {
            override fun areItemsTheSame(oldItem: Attachment<*>, newItem: Attachment<*>): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Attachment<*>, newItem: Attachment<*>): Boolean =
                    oldItem == newItem
        }
    }

}
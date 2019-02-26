package io.github.koss.mammut.feature.instance.subfeature.feed

import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.RequestManager
import io.github.koss.mammut.R
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.data.database.entities.feed.Status

class FeedAdapter(
        private val viewModelProvider: ViewModelProvider,
        private val tootCallbacks: TootCallbacks,
        private val requestManager: RequestManager,
        private val onBrokenTimelineResolved: () -> Unit
): PagedListAdapter<Status, FeedItemViewHolder>(DIFF_CALLBACK) {

    private var brokenFeed: Boolean = false

    fun setFeedBroken(isFeedBroken: Boolean) {
        if (brokenFeed != isFeedBroken) {
            brokenFeed = isFeedBroken
            if (isFeedBroken) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }

    override fun getItemId(position: Int): Long = getItem(position)?.id ?: 0L

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedItemViewHolder =
            when (viewType) {
                R.layout.view_holder_broken_timeline -> BrokenTimelineViewHolder(parent)
                else -> TootViewHolder(parent, viewModelProvider, requestManager, tootCallbacks)
            }

    override fun onBindViewHolder(holder: FeedItemViewHolder, position: Int) {
        when (holder) {
            is TootViewHolder -> {
                val current = getItem(position) ?: run {
                    holder.clear()
                    return
                }

                holder.bind(current)
            }
            is BrokenTimelineViewHolder -> {
                holder.bind(onBrokenTimelineResolved)
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when {
        position == 0 && brokenFeed -> R.layout.view_holder_broken_timeline
        else -> R.layout.view_holder_feed_item
    }

    override fun getItemCount(): Int =
            super.getItemCount() + if (brokenFeed) 1 else 0

    override fun onViewRecycled(holder: FeedItemViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is TootViewHolder -> holder.recycle()
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Status>() {
            override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean = oldItem.content == newItem.content
        }
    }
}
package io.github.koss.mammut.feed.ui.list

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.presentation.model.BrokenTimelineModel
import io.github.koss.mammut.feed.presentation.model.FeedModel
import io.github.koss.mammut.feed.presentation.model.StatusModel
import io.github.koss.mammut.feed.ui.broken.BrokenTimelineViewHolder
import io.github.koss.mammut.feed.ui.status.StatusViewHolder
import io.github.koss.mammut.feed.util.FeedCallbacks
import io.github.koss.paging.event.PagingRelay
import kotlinx.android.extensions.LayoutContainer

open class FeedItemViewHolder(
    itemView: View,
    override val containerView: View = itemView
): RecyclerView.ViewHolder(itemView), LayoutContainer

class FeedAdapter(
        private val viewModelProvider: ViewModelProvider,
        private val feedCallbacks: FeedCallbacks,
        private val pagingRelay: PagingRelay
): ListAdapter<FeedModel, FeedItemViewHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position)?.id ?: 0L

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedItemViewHolder =
            when (viewType) {
                R.layout.view_holder_broken_timeline -> BrokenTimelineViewHolder(parent)
                else -> StatusViewHolder(parent, viewModelProvider, feedCallbacks)
            }

    override fun onBindViewHolder(holder: FeedItemViewHolder, position: Int) {
        when (position) {
            0 -> pagingRelay.startOfDataDisplayed()
            itemCount - 1 -> pagingRelay.endOfDataDisplayed()
        }

        when (holder) {
            is StatusViewHolder -> {
                // Occasionally when scrolling down quickly, we can get in to race conditions
                // where binding isn't complete when an insertion or deletion occurs due to streaming.
                // This statement is a guard for that case.
                if (position == itemCount) return

                val current = getItem(position) as? StatusModel ?: return

                holder.bind(current.status)
            }
            is BrokenTimelineViewHolder -> {
                holder.bind(callbacks = feedCallbacks)
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (val item = getItem(position)) {
        BrokenTimelineModel -> R.layout.view_holder_broken_timeline
        is StatusModel -> R.layout.view_holder_feed_item
        else -> throw IllegalArgumentException("No known viewtype for item: $item")
    }

    override fun onViewDetachedFromWindow(holder: FeedItemViewHolder) {
        super.onViewDetachedFromWindow(holder)
        when (holder) {
            is StatusViewHolder -> holder.dettach()
        }
    }

    override fun onViewRecycled(holder: FeedItemViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is StatusViewHolder -> holder.recycle()
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedModel>() {
            override fun areItemsTheSame(oldItem: FeedModel, newItem: FeedModel): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FeedModel, newItem: FeedModel): Boolean = oldItem.content == newItem.content
        }
    }
}

private val FeedModel.id: Long get() = when (this) {
    BrokenTimelineModel -> 1L
    is StatusModel -> this.status.id
}
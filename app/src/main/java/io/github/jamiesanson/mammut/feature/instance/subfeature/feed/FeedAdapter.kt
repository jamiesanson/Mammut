package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import io.github.jamiesanson.mammut.data.database.entities.feed.Status

class FeedAdapter(
        private val onLoadAround: (Long) -> Unit
): PagedListAdapter<Status, TootViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TootViewHolder =
            TootViewHolder(parent)

    override fun onBindViewHolder(holder: TootViewHolder, position: Int) {
        val current = getItem(position) ?: run {
            holder.clear()
            return
        }

        loadAround(current.id)
        holder.bind(current)
    }

    private fun loadAround(id: Long) {
        onLoadAround(id)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Status>() {
            override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean = oldItem == newItem
        }
    }
}
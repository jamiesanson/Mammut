package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.RequestManager
import io.github.jamiesanson.mammut.data.database.entities.feed.Status

class FeedAdapter(
        private val onLoadAround: (Long) -> Unit,
        private val tootCallbacks: TootCallbacks,
        private val requestManager: RequestManager
): PagedListAdapter<Status, TootViewHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position)?.id ?: 0L

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TootViewHolder =
            TootViewHolder(parent)

    override fun onBindViewHolder(holder: TootViewHolder, position: Int) {
        val current = getItem(position) ?: run {
            holder.clear()
            return
        }

        loadAround(current.id)
        holder.bind(current, tootCallbacks, requestManager)
    }

    private fun loadAround(id: Long) {
        onLoadAround(id)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Status>() {
            override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean = oldItem.content == newItem.content
        }
    }
}
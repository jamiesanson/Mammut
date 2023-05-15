package io.github.koss.mammut.feature.joininstance.suggestion

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.koss.mammut.R
import io.github.koss.mammut.base.util.inflate
import io.github.koss.mammut.data.models.InstanceSearchResult
import io.github.koss.mammut.databinding.InstanceSuggestionLayoutBinding

class InstanceSuggestionAdapter(private val onResultSelected: (InstanceSearchResult) -> Unit) : ListAdapter<InstanceSearchResult, InstanceSuggestionAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(parent.inflate(R.layout.instance_suggestion_layout))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item) { onResultSelected(item) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(result: InstanceSearchResult, onClick: () -> Unit) {
            val binding = InstanceSuggestionLayoutBinding.bind(itemView)
            with (binding) {
                instanceNameTextView.text = result.name
                usersCountTextView.text = "${result.users} Users"
                root.setOnClickListener { onClick() }
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<InstanceSearchResult>() {
            override fun areItemsTheSame(oldItem: InstanceSearchResult, newItem: InstanceSearchResult): Boolean =
                    oldItem == newItem

            override fun areContentsTheSame(oldItem: InstanceSearchResult, newItem: InstanceSearchResult): Boolean =
                    oldItem.toString() == newItem.toString()
        }
    }
}
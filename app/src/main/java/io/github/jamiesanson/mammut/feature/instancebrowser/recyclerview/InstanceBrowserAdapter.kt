package io.github.jamiesanson.mammut.feature.instancebrowser.recyclerview

import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.github.jamiesanson.mammut.data.models.InstanceRegistration

/**
 * RecyclerView adapter for displaying instances
 */
class InstanceBrowserAdapter(
        private val viewModelProvider: ViewModelProvider
): ListAdapter<InstanceRegistration, InstanceViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstanceViewHolder =
            InstanceViewHolder(parent)

    override fun onBindViewHolder(holder: InstanceViewHolder, position: Int) {
        val item = getItem(position)
        val viewModel = viewModelProvider.get(item.id.toString(), InstanceCardViewModel::class.java)
        viewModel.initialise(item)

        holder.bind(viewModel)
    }

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<InstanceRegistration>() {
            override fun areItemsTheSame(oldItem: InstanceRegistration, newItem: InstanceRegistration): Boolean =
                    oldItem == newItem

            override fun areContentsTheSame(oldItem: InstanceRegistration, newItem: InstanceRegistration): Boolean =
                    oldItem.toString() == newItem.toString()
        }
    }
}
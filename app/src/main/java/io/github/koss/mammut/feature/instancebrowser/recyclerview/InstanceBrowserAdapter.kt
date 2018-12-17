package io.github.koss.mammut.feature.instancebrowser.recyclerview

import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.data.remote.response.InstanceDetail
import kotlinx.android.synthetic.main.view_holder_instance_card.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * RecyclerView adapter for displaying instances
 */
class InstanceBrowserAdapter(
        private val viewModelProvider: ViewModelProvider,
        private val onInstanceClicked: (InstanceRegistration) -> Unit,
        private val onAboutInstanceClicked: (InstanceDetail, Long) -> Unit
): ListAdapter<InstanceRegistration, InstanceViewHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstanceViewHolder =
            InstanceViewHolder(parent)

    override fun onBindViewHolder(holder: InstanceViewHolder, position: Int) {
        val item = getItem(position)
        val viewModel = viewModelProvider.get(item.id.toString(), InstanceCardViewModel::class.java)
        viewModel.initialise(item)

        holder.bind(viewModel) { onAboutInstanceClicked(it, item.id) }
        holder.itemView.parentLayout.onClick { onInstanceClicked(item) }
    }

    override fun getItemId(position: Int): Long =
            getItem(position).id

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<InstanceRegistration>() {
            override fun areItemsTheSame(oldItem: InstanceRegistration, newItem: InstanceRegistration): Boolean =
                    oldItem == newItem

            override fun areContentsTheSame(oldItem: InstanceRegistration, newItem: InstanceRegistration): Boolean =
                    oldItem.toString() == newItem.toString()
        }
    }
}
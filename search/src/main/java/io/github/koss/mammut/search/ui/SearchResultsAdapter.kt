package io.github.koss.mammut.search.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.github.koss.mammut.search.databinding.AccountViewHolderBinding
import io.github.koss.mammut.search.presentation.model.Account
import io.github.koss.mammut.search.presentation.model.SearchModel

class SearchResultsAdapter(
        private val onAccountClicked: (accountId: Long) -> Unit
): ListAdapter<SearchModel, SearchViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return when (viewType) {
            0 -> AccountViewHolder(
                binding = AccountViewHolderBinding
                        .inflate(LayoutInflater.from(parent.context), parent, false),
                onClick = onAccountClicked
            )
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        when (val item = getItem(position)) {
          is Account -> (holder as AccountViewHolder).bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        when (getItem(position)) {
            is Account -> return 0
        }
    }

    companion object {
        val DIFF_CALLBACK = object: DiffUtil.ItemCallback<SearchModel>() {
            override fun areItemsTheSame(oldItem: SearchModel, newItem: SearchModel): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: SearchModel, newItem: SearchModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
package io.github.koss.mammut.feature.joininstance.suggestion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.koss.mammut.data.models.InstanceSearchResult
import io.github.koss.mammut.databinding.InstanceSuggestionPopupWindowBinding

class InstanceSuggestionPopupWindow(
    context: Context,
    onInstanceSelected: (InstanceSearchResult) -> Unit
) : PopupWindow(InstanceSuggestionPopupWindowBinding.inflate(LayoutInflater.from(context)).root) {

    private val instanceSuggestionAdapter = InstanceSuggestionAdapter(onInstanceSelected)

    private lateinit var binding: InstanceSuggestionPopupWindowBinding

    override fun setContentView(contentView: View?) {
        super.setContentView(contentView)
        contentView?.let {
            binding = InstanceSuggestionPopupWindowBinding.bind(it)
            binding.suggestionsRecyclerView.layoutManager =
                LinearLayoutManager(contentView.context)
            binding.suggestionsRecyclerView.adapter = instanceSuggestionAdapter
        }
    }

    fun onNewResults(results: List<InstanceSearchResult>) {
        instanceSuggestionAdapter.submitList(results)
        binding.suggestionsRecyclerView.scrollToPosition(0)
    }
}
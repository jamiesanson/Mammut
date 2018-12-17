package io.github.koss.mammut.feature.joininstance.suggestion

import android.content.Context
import android.view.View
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.koss.mammut.data.models.InstanceSearchResult
import kotlinx.android.synthetic.main.instance_suggestion_popup_window.view.*

class InstanceSuggestionPopupWindow(context: Context, onInstanceSelected: (InstanceSearchResult) -> Unit) : PopupWindow(context) {

    private val instanceSuggestionAdapter = InstanceSuggestionAdapter(onInstanceSelected)

    override fun setContentView(contentView: View?) {
        super.setContentView(contentView)
        contentView?.suggestionsRecyclerView?.layoutManager = LinearLayoutManager(contentView?.context)
        contentView?.suggestionsRecyclerView?.adapter = instanceSuggestionAdapter
    }

    fun onNewResults(results: List<InstanceSearchResult>) {
        instanceSuggestionAdapter.submitList(results)
    }
}
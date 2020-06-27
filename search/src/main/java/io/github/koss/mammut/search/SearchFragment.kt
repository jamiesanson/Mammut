package io.github.koss.mammut.search

import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.util.*
import io.github.koss.mammut.search.dagger.SearchComponent
import io.github.koss.mammut.search.dagger.SearchModule
import io.github.koss.mammut.search.dagger.SearchScope
import io.github.koss.mammut.search.databinding.SearchFragmentBinding
import io.github.koss.mammut.search.presentation.state.Loaded
import io.github.koss.mammut.search.presentation.state.Loading
import io.github.koss.mammut.search.presentation.state.NoResults
import io.github.koss.mammut.search.presentation.state.SearchState
import io.github.koss.mammut.search.ui.SearchResultsAdapter
import org.jetbrains.anko.dimenAttr
import org.jetbrains.anko.support.v4.dip
import javax.inject.Inject
import javax.inject.Named

class SearchFragment: Fragment(R.layout.search_fragment) {

    private val binding by viewLifecycleLazy { SearchFragmentBinding.bind(requireView()) }

    private lateinit var viewModel: SearchViewModel

    @Inject
    @SearchScope
    lateinit var factory: MammutViewModelFactory

    @Inject
    @InstanceScope
    @Named("instance_access_token")
    lateinit var accessToken: String

    private var searchTextWatcher: TextWatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findSubcomponentFactory()
            .buildSubcomponent<SearchModule, SearchComponent>(SearchModule())
            .inject(this)

        viewModel = ViewModelProvider(requireActivity(), factory).get(accessToken, SearchViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with (binding) {
            searchInputCard.doOnApplyWindowInsets { cardView, insets, initialState ->
                cardView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = insets.systemWindowInsetTop + initialState.margins.top
                }
            }

            searchTextWatcher = searchEditText.doOnTextChanged { text, _, _, _ ->
                text?.toString()?.let(viewModel::search)
            }

            searchResultsRecyclerView.adapter = SearchResultsAdapter { accountId ->
                findRootNavController().navigate("mammut://profile/${accountId}".toUri())
            }
        }

        binding.searchResultsRecyclerView.doOnApplyWindowInsets { recyclerView, insets, _ ->
            recyclerView.updatePadding(
                top = insets.systemWindowInsetTop + dip(56) + dip(8),
                bottom = dip(96)
            )

            recyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val actionBarSize = requireContext().dimenAttr(R.attr.actionBarSize)
                topMargin = -(insets.systemWindowInsetTop + actionBarSize)
            }
        }

        viewModel.state.observe(this, ::onStateChanged)
    }

    private fun onStateChanged(state: SearchState) {
        // TODO - Improve animations when smoothing things out
        when (state) {
            is NoResults -> {
                with (binding) {
                    noResultsTextView.isVisible = true

                    progressBar.isGone = true
                    searchResultsRecyclerView.isGone = true

                    updateSearchQuery(state.query)
                }
            }
            is Loading -> {
                with (binding) {
                    progressBar.isVisible = true

                    noResultsTextView.isGone = true
                    searchResultsRecyclerView.isGone = true

                    updateSearchQuery(state.query)
                }
            }
            is Loaded -> {
                with (binding) {
                    noResultsTextView.isGone = true
                    progressBar.isGone = true

                    searchResultsRecyclerView.isVisible = true
                    (searchResultsRecyclerView.adapter as SearchResultsAdapter)
                            .submitList(state.results)

                    updateSearchQuery(state.query)
                }
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        with (binding) {
            if (searchEditText.text.toString() != query) {
                searchEditText.removeTextChangedListener(searchTextWatcher)
                searchEditText.setText(query)
                searchEditText.addTextChangedListener(searchTextWatcher)
            }
        }
    }
}
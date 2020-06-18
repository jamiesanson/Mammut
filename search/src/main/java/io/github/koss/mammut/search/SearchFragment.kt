package io.github.koss.mammut.search

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.util.findSubcomponentFactory
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.search.dagger.SearchComponent
import io.github.koss.mammut.search.dagger.SearchModule
import io.github.koss.mammut.search.dagger.SearchScope
import io.github.koss.mammut.search.databinding.SearchFragmentBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findSubcomponentFactory()
            .buildSubcomponent<SearchModule, SearchComponent>(SearchModule())
            .inject(this)

        viewModel = ViewModelProvider(requireActivity(), factory).get(accessToken, SearchViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchInputCard.doOnApplyWindowInsets { cardView, insets, initialState ->
            cardView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.systemWindowInsetTop + initialState.margins.top
            }
        }
    }
}
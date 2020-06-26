package io.github.koss.mammut.feed.ui.hashtag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.base.dagger.scope.ApplicationScope
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.base.util.findSubcomponentFactory
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.dagger.FeedComponent
import io.github.koss.mammut.feed.dagger.FeedModule
import io.github.koss.mammut.feed.databinding.HashtagFragmentBinding
import io.github.koss.mammut.feed.ui.FeedFragment
import io.github.koss.mammut.feed.ui.FeedFragmentArgs
import kotlinx.android.synthetic.main.hashtag_fragment.*
import javax.inject.Inject

class HashtagFragment: Fragment(R.layout.hashtag_fragment) {

    private val binding by viewLifecycleLazy { HashtagFragmentBinding.bind(requireView()) }

    private val args by navArgs<HashtagFragmentArgs>()

    private val feedType by lazy { FeedType.Hashtag(tag = args.tagName)}

    @Inject
    @ApplicationScope
    lateinit var themeEngine: ThemeEngine

    private var componentCache = mutableMapOf<FeedType, FeedComponent>()

    private fun retrieveComponent(feedType: FeedType): FeedComponent =
            when (val cachedComponent = componentCache[feedType]) {
                null -> {
                    findSubcomponentFactory()
                            .buildSubcomponent<FeedModule, FeedComponent>(FeedModule(feedType))
                            .also {
                                componentCache[feedType] = it
                            }
                }
                else -> cachedComponent
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retrieveComponent(feedType)
                .inject(this)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NavigationUI.setupWithNavController(binding.toolbar, findNavController())

        binding.toolbar.title = "#" + args.tagName

        binding.collapsingLayout.doOnApplyWindowInsets { collapsingLayout, insets, _ ->
            collapsingLayout.updatePadding(top = insets.systemWindowInsetTop)
        }

        themeEngine.applyFontToCollapsingLayout(binding.collapsingLayout)

        val feedFragmentArgs = FeedFragmentArgs(feedType = feedType)
        val feedFragment = FeedFragment().apply {
            arguments = feedFragmentArgs.toBundle()
        }

        childFragmentManager.beginTransaction()
                .replace(feedFragmentContainer.id, feedFragment)
                .commitNow()
    }
}
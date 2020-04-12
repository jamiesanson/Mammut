package io.github.koss.mammut.feed.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.base.dagger.scope.FeedScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.util.findSubcomponentFactory
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.dagger.FeedComponent
import io.github.koss.mammut.feed.dagger.FeedModule
import io.github.koss.mammut.feed.databinding.FeedFragmentBinding
import io.github.koss.mammut.feed.domain.FeedType
import io.github.koss.mammut.feed.presentation.FeedViewModel
import io.github.koss.mammut.feed.presentation.event.FeedEvent
import io.github.koss.mammut.feed.presentation.event.ItemStreamed
import io.github.koss.mammut.feed.presentation.state.FeedState
import io.github.koss.mammut.feed.presentation.state.Loaded
import io.github.koss.mammut.feed.presentation.state.LoadingAll
import io.github.koss.mammut.feed.ui.list.FeedAdapter
import io.github.koss.mammut.feed.ui.view.NetworkIndicator
import io.github.koss.mammut.feed.util.FeedCallbacks
import io.github.koss.paging.event.PagingRelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.support.v4.dip
import javax.inject.Inject
import javax.inject.Named

// This is due to a limitation of the navigation library, allowing us to use default parcelable args
class HomeFeedFragment : FeedFragment() {
    override var feedType: FeedType = FeedType.Home
}

class LocalFeedFragment : FeedFragment() {
    override var feedType: FeedType = FeedType.Local
}

class FederatedFeedFragment : FeedFragment() {
    override var feedType: FeedType = FeedType.Federated
}

/**
 * This class is the Controller responsible for displaying a feed.
 *
 * The things needed to be added from the old one:
 * * Pull to refresh
 * * Full screen loading state
 * * Don't show the loading indicator when at the top
 * * Position persistence
 */
open class FeedFragment : Fragment(R.layout.feed_fragment), FeedCallbacks {

    private val binding by viewLifecycleLazy { FeedFragmentBinding.bind(requireView()) }

    protected open lateinit var feedType: FeedType

    private lateinit var viewModel: FeedViewModel

    @Inject
    @FeedScope
    lateinit var factory: MammutViewModelFactory

    @Inject
    @FeedScope
    lateinit var pagingRelay: PagingRelay

    @Inject
    @Named("instance_access_token")
    lateinit var accessToken: String

    private val uniqueId: String by lazy {
        "$accessToken$feedType"
    }

    private val feedModule: FeedModule by lazy {
        FeedModule(feedType)
    }

    private val tootButtonHidden: Boolean
        get() = binding.newTootButton.translationY != 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findSubcomponentFactory()
                .buildSubcomponent<FeedModule, FeedComponent>(feedModule)
                .inject(this)

        viewModel = ViewModelProvider(context as AppCompatActivity, factory)
                .get(uniqueId, FeedViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeToRefresh()
        handleInsets()

        viewModel.state.observe(viewLifecycleOwner, Observer {
            processState(it)
        })
        viewModel.event.observe(viewLifecycleOwner, Observer {
            handleEvent(it)
        })

        // Attach the network indicator TODO - Do this more elegantly
        // NetworkIndicator().attach(view as ViewGroup, viewLifecycleOwner)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = FeedAdapter(
                viewModelProvider = ViewModelProvider(activity as AppCompatActivity, factory),
                feedCallbacks = this,
                pagingRelay = pagingRelay
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // If we've scrolled to the top of the recyclerView, hide the new toots indicator
                if (newState == RecyclerView.SCROLL_STATE_IDLE && recyclerView.isNearTop()) {
                    if (!tootButtonHidden) {
                        hideNewTootsIndicator()
                    }
                }
            }
        })
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.reload()
        }
    }

    private fun handleInsets() {
        binding.recyclerView.doOnApplyWindowInsets { view, insets, _ ->
            view.updatePadding(top = insets.systemWindowInsetTop)
        }

        binding.newTootButton.doOnApplyWindowInsets { view, insets, _ ->
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.systemWindowInsetTop
            }
        }
    }

    private fun processState(state: FeedState) {
        when (state) {
            is LoadingAll -> showLoadingAll()
            is Loaded -> showLoaded(state)
        }
    }

    private fun handleEvent(event: FeedEvent) {
        when (event) {
            ItemStreamed -> onItemStreamed()
        }
    }

    private fun onItemStreamed() {
        if (binding.recyclerView.isNearTop()) {
            // Wait a little while for the insert to occur
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                delay(100)
                withContext(Dispatchers.Main) {
                    if (viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        binding.recyclerView.smoothScrollToPosition(0)
                    }
                }
            }
        } else {
            if (tootButtonHidden) {
                showNewTootsIndicator()
            }
        }
    }

    private fun showLoadingAll() {
        // Only show the full progress bar if the swipe refresh layout isn't refreshing
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.progressBar.isVisible = true
        }
        binding.bottomLoadingIndicator.isVisible = false
        binding.topLoadingIndicator.isVisible = false
    }

    private fun showLoaded(state: Loaded) {
        binding.topLoadingIndicator.isVisible = state.loadingAtFront
        binding.bottomLoadingIndicator.isVisible = state.loadingAtEnd

        binding.swipeRefreshLayout.isRefreshing = false

        binding.progressBar.isVisible = false

        binding.swipeRefreshLayout.isRefreshing = false

        (binding.recyclerView.adapter as? FeedAdapter)?.submitList(state.items)
        binding.recyclerView.doOnLayout {
            if (state.initialPosition > 0 || state.initialPosition < state.items.size) {
                binding.recyclerView.scrollToPosition(state.initialPosition)
            }
        }
    }

    private fun showNewTootsIndicator(animate: Boolean = true) {
        if (animate) {
            binding.newTootButton.animate()
                    .translationY(0F)
                    .setInterpolator(OvershootInterpolator())
                    .setDuration(300L)
                    .start()
        } else {
            binding.newTootButton.translationY = 0F
        }

        binding.newTootButton.setOnClickListener {
            hideNewTootsIndicator()
            binding.recyclerView.scrollToPosition(0)
        }
    }

    private fun hideNewTootsIndicator(animate: Boolean = true) {
        if (animate) {
            binding.newTootButton.animate()
                    .translationY(-(binding.newTootButton.y + binding.newTootButton.height + dip(50)))
                    .setInterpolator(AccelerateInterpolator())
                    .setDuration(150L)
                    .start()
        } else {
            binding.newTootButton.translationY = -(binding.newTootButton.y + binding.newTootButton.height + dip(50))
        }
    }

    private fun RecyclerView.isNearTop(): Boolean =
            (layoutManager as LinearLayoutManager?)?.run {
                return@run findFirstVisibleItemPosition() < 3
            } ?: false


    override fun onProfileClicked(account: Account) {
        TODO("Not yet implemented")
    }

    override fun onPhotoClicked(imageView: ImageView, photoUrl: String) {
        TODO("Not yet implemented")
    }

    override fun onTootClicked(status: Status) {
        TODO("Not yet implemented")
    }

    override fun onReloadClicked() {
        TODO("Not yet implemented")
    }
}
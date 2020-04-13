package io.github.koss.mammut.feed.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.base.dagger.scope.FeedScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.navigation.NavigationEvent
import io.github.koss.mammut.base.navigation.NavigationEventBus
import io.github.koss.mammut.base.util.findSubcomponentFactory
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.dagger.FeedComponent
import io.github.koss.mammut.feed.dagger.FeedModule
import io.github.koss.mammut.feed.databinding.FeedFragmentBinding
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.feed.presentation.FeedViewModel
import io.github.koss.mammut.feed.presentation.event.FeedEvent
import io.github.koss.mammut.feed.presentation.event.ItemStreamed
import io.github.koss.mammut.feed.presentation.state.FeedState
import io.github.koss.mammut.feed.presentation.state.Loaded
import io.github.koss.mammut.feed.presentation.state.LoadingAll
import io.github.koss.mammut.feed.ui.list.FeedAdapter
import io.github.koss.mammut.feed.util.FeedCallbacks
import io.github.koss.paging.event.PagingRelay
import javax.inject.Inject
import javax.inject.Named

// This is due to a limitation of the navigation library, allowing us to use default parcelable args
class HomeFeedFragment : FeedFragment() {
    override var feedType: FeedType = FeedType.Home
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

    @Inject
    @FeedScope
    lateinit var factory: MammutViewModelFactory

    @Inject
    @FeedScope
    lateinit var pagingRelay: PagingRelay

    @Inject
    @Named("instance_access_token")
    lateinit var accessToken: String

    @Inject
    lateinit var navigationBus: NavigationEventBus

    private val uniqueId: String get() =
        "$accessToken$feedType"

    private val binding by viewLifecycleLazy { FeedFragmentBinding.bind(requireView()) }

    private var currentOffScreenCount: Int = 0
    private var pendingStreamItem: Boolean = false

    private val stateObserver = Observer<FeedState> { processState(it) }
    private val eventObserver = Observer<FeedEvent> { handleEvent(it) }

    protected open lateinit var feedType: FeedType

    private lateinit var viewModel: FeedViewModel

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
        initialiseDependencies()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeToRefresh()
        handleInsets()

        viewModel.state.observe(viewLifecycleOwner, stateObserver)
        viewModel.event.observe(viewLifecycleOwner, eventObserver)

        navigationBus.events.observe(viewLifecycleOwner, Observer {
            when (val incomingEvent = it.getContentIfNotHandled()) {
                is NavigationEvent.Feed.TypeChanged ->
                    swapFeedType(incomingEvent.newFeedType)
            }
        })
    }

    private fun initialiseDependencies() {
        retrieveComponent(feedType)
                .inject(this)

        viewModel = ViewModelProvider(context as AppCompatActivity, factory)
                .get(uniqueId, FeedViewModel::class.java)
    }

    private fun swapFeedType(newFeedType: FeedType) {
        // Remove observers
        viewModel.state.removeObserver(stateObserver)
        viewModel.event.removeObserver(eventObserver)

        // Clear adapter
        (binding.recyclerView.adapter as FeedAdapter)
                .submitList(emptyList())

        showLoadingAll()

        // Swap dependencies
        feedType = newFeedType
        initialiseDependencies()

        // Re-add observers
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
        viewModel.event.observe(viewLifecycleOwner, eventObserver)
    }

    private fun updateBadgeCount() {
        val newCount = (binding.recyclerView.layoutManager as LinearLayoutManager)
                .findFirstCompletelyVisibleItemPosition()

        if (newCount == currentOffScreenCount) return

        currentOffScreenCount = newCount
        navigationBus.sendEvent(NavigationEvent.Feed.OffscreenCountChanged(
                newCount = currentOffScreenCount
        ))
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
                updateBadgeCount()
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
    }

    private fun processState(state: FeedState) {
        when (state) {
            is LoadingAll -> showLoadingAll()
            is Loaded -> showLoaded(state)
        }
    }

    private fun handleEvent(event: FeedEvent) {
        when (event) {
            ItemStreamed -> pendingStreamItem = true
        }
    }

    private fun onItemStreamed() {
        if (binding.recyclerView.isNearTop()) {
            binding.recyclerView.doOnNextLayout {
                (it as RecyclerView).smoothScrollToPosition(0)
            }
        } else {
            updateBadgeCount()
        }

        pendingStreamItem = false
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

        binding.progressBar.isVisible = false

        binding.swipeRefreshLayout.isRefreshing = false

        (binding.recyclerView.adapter as? FeedAdapter)?.submitList(state.items)

        if (pendingStreamItem) {
            onItemStreamed()
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
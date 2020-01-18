package io.github.koss.mammut.feed.ui

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.dagger.scope.FeedScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.navigation.FullScreenPhotoHandler
import io.github.koss.mammut.base.navigation.NavigationHub
import io.github.koss.mammut.base.navigation.ReselectListener
import io.github.koss.mammut.base.util.arg
import io.github.koss.mammut.base.util.comingSoon
import io.github.koss.mammut.base.util.findSubcomponentFactory
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.retained
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.dagger.FeedComponent
import io.github.koss.mammut.feed.dagger.FeedModule
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
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.controller_feed.*
import kotlinx.android.synthetic.main.controller_feed.view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onScrollChange
import org.jetbrains.anko.support.v4.onRefresh
import javax.inject.Inject

const val ARG_ACCESS_TOKEN = "access_token"
const val ARG_TYPE = "feed_type"

/**
 * This class is the Controller responsible for displaying a feed.
 *
 * The things needed to be added from the old one:
 * * Pull to refresh
 * * Full screen loading state
 * * Don't show the loading indicator when at the top
 * * Position persistence
 */
class FeedController(args: Bundle) : BaseController(args), ReselectListener, FeedCallbacks {

    private lateinit var viewModel: FeedViewModel

    @Inject
    @FeedScope
    lateinit var factory: MammutViewModelFactory

    @Inject
    @FeedScope
    lateinit var pagingRelay: PagingRelay

    private val accessToken: String by arg(ARG_ACCESS_TOKEN)
    private val type: FeedType by arg(ARG_TYPE)

    private val uniqueId: String by lazy {
        "$accessToken$type"
    }

    private val feedModule: FeedModule by retained(key = ::uniqueId) {
        FeedModule(type, uniqueId)
    }

    private val tootButtonHidden: Boolean
        get() = newTootButton?.translationY != 0f

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        findSubcomponentFactory()
                .buildSubcomponent<FeedModule, FeedComponent>(feedModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context as AppCompatActivity, factory).get(uniqueId, FeedViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_feed, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        super.initialise(savedInstanceState)

        // Ensure all stuck references are cleared (Big hack, sorry)
        clearFindViewByIdCache()

        // Only show the progress bar if we're displaying this controller the first time
        if (savedInstanceState == null) {
            // The following ensures we only change animate the display of the progress bar if
            // showing for the first time, i.e don't transition again after a rotation
            val transition = AutoTransition()
            transition.excludeTarget(recyclerView, true)
            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)

            progressBar.visibility = View.VISIBLE
        }

        setupRecyclerView()
        setupSwipeToRefresh()

        NetworkIndicator(view!!.context).attach(view as ViewGroup, this)

        swipeRefreshLayout.onRefresh {
            viewModel.reload()
        }

        recyclerView?.doOnApplyWindowInsets { view, insets, _ ->
            view.updatePadding(top = insets.systemWindowInsetTop)
        }

        newTootButton?.doOnApplyWindowInsets { view, insets, _ ->
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.systemWindowInsetTop
            }
        }

        viewModel.state.observe(this) {
            containerView ?: return@observe
            processState(it)
        }
        viewModel.event.observe(this) {
            containerView ?: return@observe
            handleEvent(it)
        }
    }

    override fun onTabReselected() {
        // If we're scrolled to the top reload else scroll up
        if (recyclerView?.computeVerticalScrollOffset() == 0) {
            // Show the swipe to refresh loading indicator
            swipeRefreshLayout.isRefreshing = true
            viewModel.reload()
        } else {
            recyclerView?.smoothScrollToPosition(0)
        }
    }

    override fun onProfileClicked(account: Account) {
        (parentController as? NavigationHub)?.pushProfileController(account)
    }

    override fun onPhotoClicked(imageView: ImageView, photoUrl: String) {
        (parentController as? FullScreenPhotoHandler)?.displayFullScreenPhoto(imageView, photoUrl)
    }

    override fun onTootClicked(status: Status) {
        comingSoon()
    }

    override fun onReloadClicked() {
        viewModel.reload()
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        val state = (view.recyclerView?.layoutManager as? LinearLayoutManager)?.onSaveInstanceState()
        outState.putParcelable(STATE_LAYOUT_MANAGER, state)
        outState.putBoolean(STATE_NEW_TOOTS_VISIBLE, !tootButtonHidden)
        viewModel.savePageState((recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        savedViewState.let(::restoreAdapterState)
        savedViewState.let(::restoreNewTootIndicatorState)
    }

    private fun restoreNewTootIndicatorState(savedInstanceState: Bundle) {
        containerView?.doOnLayout {
            if (savedInstanceState.getBoolean(STATE_NEW_TOOTS_VISIBLE)) {
                showNewTootsIndicator(animate = false)
            } else {
                hideNewTootsIndicator(animate = false)
            }
        }
    }

    private fun restoreAdapterState(savedInstanceState: Bundle) {
        savedInstanceState
            .getParcelable<Parcelable>(STATE_LAYOUT_MANAGER)
            ?.let { state ->
                (recyclerView.layoutManager as? LinearLayoutManager)?.onRestoreInstanceState(state)
            }
    }

    private fun setupRecyclerView() {
        if (recyclerView.adapter == null) {
            recyclerView.adapter = FeedAdapter(
                viewModelProvider = ViewModelProviders.of(activity as AppCompatActivity, factory),
                feedCallbacks = this,
                pagingRelay = pagingRelay
            )
        }

        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(view!!.context)
        }

        recyclerView.onScrollChange { _, _, _, _, _ ->
            containerView ?: return@onScrollChange
            
            // If we've scrolled to the top of the recyclerView, hide the new toots indicator
            if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE && recyclerView.isNearTop()) {
                if (newTootButton.translationY == 0F) {
                    hideNewTootsIndicator()
                }
            }
        }
    }

    private fun setupSwipeToRefresh() {
        swipeRefreshLayout.onRefresh {
            viewModel.reload()
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
        if (recyclerView.isNearTop()) {
            // Wait a little while for the insert to occur
            launch {
                delay(100)
                if (recyclerView?.isAttachedToWindow == true) {
                    containerView?.recyclerView?.smoothScrollToPosition(0)
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
        if (!swipeRefreshLayout.isRefreshing) {
            progressBar.isVisible = true
        }
        bottomLoadingIndicator.isVisible = false
        topLoadingIndicator.isVisible = false
    }

    private fun showLoaded(state: Loaded) {
        // Ensure all stuck references are cleared (Big hack, sorry)
        clearFindViewByIdCache()

        topLoadingIndicator.isVisible = state.loadingAtFront
        bottomLoadingIndicator.isVisible = state.loadingAtEnd

        swipeRefreshLayout.isRefreshing = false

        progressBar.isVisible = false

        swipeRefreshLayout.isRefreshing = false

        (recyclerView?.adapter as? FeedAdapter)?.submitList(state.items)
        recyclerView?.doOnLayout {
            if (state.initialPosition > 0 || state.initialPosition < state.items.size) {
                recyclerView?.scrollToPosition(state.initialPosition)
            }
        }
    }

    private fun showNewTootsIndicator(animate: Boolean = true) {
        if (animate) {
            newTootButton.animate()
                .translationY(0F)
                .setInterpolator(OvershootInterpolator())
                .setDuration(300L)
                .start()
        } else {
            newTootButton.translationY = 0F
        }

        newTootButton.onClick {
            hideNewTootsIndicator()
            recyclerView?.scrollToPosition(0)
        }
    }

    private fun hideNewTootsIndicator(animate: Boolean = true) {
        if (animate) {
            newTootButton.animate()
                .translationY(-(newTootButton.y + newTootButton.height))
                .setInterpolator(AccelerateInterpolator())
                .setDuration(150L)
                .start()
        } else {
            newTootButton.translationY = -(newTootButton.y + newTootButton.height)
        }
    }

    private fun RecyclerView.isNearTop(): Boolean =
        (layoutManager as LinearLayoutManager?)?.run {
            return@run findFirstVisibleItemPosition() < 3
        } ?: false

    companion object {
        @JvmStatic
        fun newInstance(type: FeedType, accessToken: String): FeedController =
                FeedController(bundleOf(
                        ARG_TYPE to type,
                        ARG_ACCESS_TOKEN to accessToken
                ))

        private const val STATE_LAYOUT_MANAGER = "state_layout_manager"
        private const val STATE_NEW_TOOTS_VISIBLE = "new_toots_visible"
    }
}
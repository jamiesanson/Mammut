package io.github.koss.mammut.feature.instance.subfeature.feed

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
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.component.retention.retained
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.extension.comingSoon
import io.github.koss.mammut.extension.instanceComponent
import io.github.koss.mammut.extension.observe
import io.github.koss.mammut.extension.snackbar
import io.github.koss.mammut.feature.feedpaging.FeedState
import io.github.koss.mammut.feature.instance.subfeature.FullScreenPhotoHandler
import io.github.koss.mammut.feature.instance.subfeature.feed.dagger.FeedModule
import io.github.koss.mammut.feature.instance.subfeature.feed.dagger.FeedScope
import io.github.koss.mammut.feature.feedpaging.NetworkState
import io.github.koss.mammut.feature.instance.subfeature.navigation.ReselectListener
import io.github.koss.mammut.feature.instance.subfeature.profile.ProfileController
import io.github.koss.mammut.feature.network.NetworkIndicator
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_feed.*
import kotlinx.android.synthetic.main.controller_feed.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.saket.inboxrecyclerview.executeOnMeasure
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onScrollChange
import javax.inject.Inject
import kotlin.run

/**
 * Controller used to display a feed. TODO - This needs to be generic enough to be started with
 * *any* pageable status endpoint. So far, I think it is, but I can't be sure.
 */
@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class FeedController(args: Bundle) : BaseController(args), ReselectListener, TootCallbacks {

    private lateinit var viewModel: FeedViewModel

    @Inject
    @FeedScope
    lateinit var factory: MammutViewModelFactory

    @Inject
    @ApplicationScope
    lateinit var networkIndicator: NetworkIndicator

    private val tootButtonHidden: Boolean
        get() = newTootButton?.translationY != 0f

    private val type: FeedType
        get() = args.getParcelable(FeedController.ARG_TYPE)
                ?: throw IllegalArgumentException("Missing feed attachmentType for feed fragment")

    private val instanceAccessToken: String
        get() = instanceComponent().accessToken()

    private val uniqueId: String by lazy {
        "$instanceAccessToken$type"
    }

    private val feedModule: FeedModule by retained(key = ::uniqueId) {
        FeedModule(type)
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        instanceComponent()
                .plus(feedModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context as AppCompatActivity, factory).get(uniqueId, FeedViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_feed, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        // Turns out, in some of Android's new launchers `onSaveInstanceState` can be called without ever
        // being restored. As Conductor doesn't account for this, we need to check against the view to
        // make sure we're not re-initialising things.
        if (view?.recyclerView?.adapter?.itemCount ?: 0 > 0) return

        initialiseRecyclerView()

        // Only show the progress bar if we're displaying this controller the first time
        if (savedInstanceState == null) {
            // The following ensures we only change animate the display of the progress bar if
            // showing for the first time, i.e don't transition again after a rotation
            val transition = AutoTransition()
            transition.excludeTarget(recyclerView, true)
            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)

            progressBar.visibility = View.VISIBLE
        }

        newTootButton.executeOnMeasure {
            if (savedInstanceState?.getBoolean(STATE_NEW_TOOTS_VISIBLE) == true) {
                showNewTootsIndicator(animate = false)
            } else {
                hideNewTootsIndicator(animate = false)
            }
        }

        networkIndicator.attach(view as ViewGroup, this)

        viewModel.feedData.let {
            it.refreshState.observe(this, ::onRefreshStateChanged)
            it.networkState.observe(this, ::onNetworkStateChanged)
            it.pagedList.observe(this, ::onListAvailable)
            it.state.observe(this, ::onFeedStateChanged)
        }

        viewModel.onStreamedResult.observe(this) {
            it.getContentIfNotHandled()?.let { items ->
                if (items.isNotEmpty()) {
                    onResultStreamed()
                }
            }
        }
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
    }

    override fun onTabReselected() {
        recyclerView?.smoothScrollToPosition(0)
    }

    override fun onProfileClicked(account: Account) {
        router.pushController(
                RouterTransaction
                        .with(ProfileController.newInstance(account)
                                .apply {
                                    targetController = this@FeedController.parentController
                                })
                        .pushChangeHandler(FadeChangeHandler())
                        .popChangeHandler(FadeChangeHandler()))
    }

    override fun onPhotoClicked(imageView: ImageView, photoUrl: String) {
        (parentController as? FullScreenPhotoHandler)?.displayFullScreenPhoto(imageView, photoUrl)
    }

    override fun onTootClicked(status: Status) {
        comingSoon()
    }

    private fun initialiseRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(view!!.context)

        recyclerView.adapter = FeedAdapter(
                viewModelProvider = ViewModelProviders.of(view!!.context as AppCompatActivity, factory),
                tootCallbacks = this,
                onBrokenTimelineResolved = viewModel::onBrokenTimelineResolved)

        recyclerView.itemAnimator?.addDuration = 150L

        recyclerView.onScrollChange { _, _, _, _, _ ->
            containerView ?: return@onScrollChange
            recyclerView ?: return@onScrollChange
            newTootButton ?: return@onScrollChange

            // If we've scrolled to the top of the recyclerView, hide the new toots indicator
            if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE && recyclerView.isNearTop()) {
                if (newTootButton.translationY == 0F) {
                    hideNewTootsIndicator()
                }
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun onFeedStateChanged(feedState: FeedState) {
        when (feedState) {
            FeedState.StreamingFromTop -> {
                // Disable pull to fresh when streaming
                swipeRefreshLayout.isEnabled = false
                (recyclerView.adapter as FeedAdapter).setFeedBroken(false)
            }
            FeedState.BrokenTimeline -> {
                // Insert to front of adapter
                swipeRefreshLayout.isEnabled = true
                (recyclerView.adapter as FeedAdapter).setFeedBroken(true)

                if (recyclerView.isNearTop()) {
                    recyclerView.scrollToPosition(0)
                }
            }
            FeedState.PagingUpwards -> {
                (recyclerView.adapter as FeedAdapter).setFeedBroken(false)
            }
        }
    }

    private fun onRefreshStateChanged(refreshState: NetworkState) {
        swipeRefreshLayout.isRefreshing = when (refreshState) {
            is NetworkState.Running -> true
            NetworkState.Loaded -> false
            is NetworkState.Error -> {
                snackbar(refreshState.message)
                false
            }
        }
    }

    private fun onNetworkStateChanged(networkState: NetworkState) {
        when {
            networkState is NetworkState.Running && viewModel.feedData.pagedList.value == null -> {
                progressBar.isVisible = true
            }
            networkState is NetworkState.Running -> {
                // Show start and end loading indicators
                if (recyclerView.isNearTop()) {
                    topLoadingIndicator.isVisible = networkState.start
                }

                if (recyclerView.isNearBottom()) {
                    bottomLoadingIndicator.isVisible = networkState.end
                }
            }
            networkState is NetworkState.Loaded || networkState is NetworkState.Error -> {
                topLoadingIndicator.isVisible = false
                bottomLoadingIndicator.isVisible = false
            }
        }
    }

    private fun onInitialLoad() {
        viewModel.getPreviousPosition()?.let { pos ->
            recyclerView.scrollToPosition(pos)
        }

        // Handle the refreshed event after submitting the list
        viewModel.feedData.refreshState.value?.let { state ->
            if (state is NetworkState.Loaded && recyclerView.isNearTop() && viewModel.shouldScrollOnFirstLoad) {
                recyclerView.smoothScrollToPosition(0)
            }
        }
    }

    private fun onListAvailable(pagedList: PagedList<Status>) {
        val isFirstLoad = (recyclerView?.adapter as FeedAdapter?)?.currentList == null
        (recyclerView?.adapter as FeedAdapter?)?.submitList(pagedList)

        if (isFirstLoad) {
            onInitialLoad()
        }

        if (pagedList.isNotEmpty()) {
            progressBar.isVisible = false
            emptyStateView.isVisible = false
            emptyStateView.pauseAnimation()
        } else {
            // TODO - This logic is naf. We can have an empty state with streaming, we just need to
            // do some magic to keep track to previous timings of requests.
            if (!feedModule.provideType().supportsStreaming) {
                emptyStateView.isVisible = true
                emptyStateView.playAnimation()
            } else {
                emptyStateView.isVisible = false
                emptyStateView.pauseAnimation()
            }

            progressBar.isVisible = false
        }
    }

    private fun onResultStreamed() {
        if (recyclerView.isNearTop()) {
            launch(Dispatchers.Main) {
                delay(100)
                containerView?.recyclerView?.smoothScrollToPosition(0)
            }
        } else {
            if (tootButtonHidden) {
                showNewTootsIndicator()
            }
        }

        if (emptyStateView.isVisible) {
            emptyStateView.isVisible = false
            emptyStateView.pauseAnimation()
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
            recyclerView?.smoothScrollToPosition(0)
        }
    }

    private fun hideNewTootsIndicator(animate: Boolean = true) {
        containerView ?: return

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

    private fun restoreAdapterState(savedInstanceState: Bundle) {
        savedInstanceState
                .getParcelable<Parcelable>(STATE_LAYOUT_MANAGER)
                ?.let { state ->
                    (recyclerView.layoutManager as? LinearLayoutManager)?.onRestoreInstanceState(state)
                }
    }

    private fun RecyclerView.isNearTop(): Boolean =
            (layoutManager as LinearLayoutManager?)?.run {
                return@run findFirstVisibleItemPosition() < 3
            } ?: false

    private fun RecyclerView.isNearBottom(): Boolean =
            (layoutManager as LinearLayoutManager?)?.run {
                return@run findFirstVisibleItemPosition() >= (recyclerView.adapter?.itemCount
                        ?: Int.MAX_VALUE) - 6
            } ?: false

    companion object {

        @JvmStatic
        fun newInstance(type: FeedType): FeedController =
                FeedController(bundleOf(
                        ARG_TYPE to type
                ))

        private const val ARG_TYPE = "arg_feedback_type"
        private const val STATE_LAYOUT_MANAGER = "state_layout_manager"
        private const val STATE_NEW_TOOTS_VISIBLE = "new_toots_visible"
    }
}
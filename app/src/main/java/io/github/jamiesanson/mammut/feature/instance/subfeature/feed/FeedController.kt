package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bluelinelabs.conductor.RouterTransaction
import com.bumptech.glide.RequestManager
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.GlideApp
import io.github.jamiesanson.mammut.component.retention.retained
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.data.models.Account
import io.github.jamiesanson.mammut.extension.comingSoon
import io.github.jamiesanson.mammut.extension.observe
import io.github.jamiesanson.mammut.extension.snackbar
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instance.subfeature.FullScreenPhotoHandler
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedModule
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedScope
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.paging.NetworkState
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.BaseController
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.ProfileController
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
class FeedController(args: Bundle) : BaseController(args), TootCallbacks {

    private lateinit var viewModel: FeedViewModel

    private lateinit var requestManager: RequestManager

    @Inject
    @FeedScope
    lateinit var factory: MammutViewModelFactory

    // REGION DYNAMIC STATE
    private var adapterStateRestored: Boolean = false
    private var firstSmoothScrollSkipped: Boolean = false
    private var tootButtonVisible: Boolean = false
    // END REGION

    private val type: FeedType
        get() = args.getParcelable(FeedController.ARG_TYPE)
                ?: throw IllegalArgumentException("Missing feed attachmentType for feed fragment")

    private val feedModule: FeedModule by retained(key = {
        type.toString()
    }) {
        FeedModule(type)
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as InstanceActivity)
                .component
                .plus(feedModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context, factory).get(type.toString(), FeedViewModel::class.java)
        requestManager = GlideApp.with(context)
    }

    override fun onContextUnavailable() {
        super.onContextUnavailable()
        // Clear the request manager
        // NOTE - The following could throw an exception if nothing's used the manager,
        // so try-catch it
        try {
            requestManager.onDestroy()
        } catch (e: Exception) {
            // no-op
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_feed, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        adapterStateRestored = false
        firstSmoothScrollSkipped = false

        recyclerView.layoutManager = LinearLayoutManager(view!!.context).apply {
            isItemPrefetchEnabled = true
            initialPrefetchItemCount = 20
        }
        recyclerView.adapter = FeedAdapter(this, requestManager)

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

        recyclerView.onScrollChange { _, _, _, _, _ ->
            containerView ?: return@onScrollChange
            recyclerView ?: return@onScrollChange
            newTootButton ?: return@onScrollChange

            if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE && recyclerView.isNearTop()) {
                if (newTootButton.translationY == 0F) {
                    hideNewTootsIndicator()
                }
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        viewModel.feedData.refreshState.observe(this) {
            swipeRefreshLayout.isRefreshing = when (it) {
                NetworkState.Running -> true
                NetworkState.Loaded -> false
                is NetworkState.Error -> {
                    snackbar(it.message)
                    false
                }
            }
        }

        viewModel.feedData.networkState.observe(this) {
            when {
                it is NetworkState.Running && viewModel.feedData.pagedList.value == null -> {
                    progressBar.isVisible = true
                }
            }
        }

        viewModel.feedData.pagedList.observe(this) {
            (recyclerView?.adapter as FeedAdapter?)?.submitList(it)

            if (progressBar.visibility == View.VISIBLE && it.size != 0) {
                progressBar.visibility = View.GONE
            }

            // Handle the refreshed event after submitting the list
            viewModel.feedData.refreshState.value?.let { state ->
                if (state is NetworkState.Loaded && recyclerView.isNearTop() && viewModel.shouldScrollOnFirstLoad) {
                    recyclerView.smoothScrollToPosition(0)
                }
            }
        }

        savedInstanceState?.let(::restoreAdapterState)
        adapterStateRestored = true

        observeStream()
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        val state = (view.recyclerView?.layoutManager as? LinearLayoutManager)?.onSaveInstanceState()
        outState.putParcelable(STATE_LAYOUT_MANAGER, state)
        outState.putBoolean(STATE_NEW_TOOTS_VISIBLE, tootButtonVisible)
    }

    override fun onTabReselected() {
        recyclerView?.smoothScrollToPosition(0)
    }

    override fun onProfileClicked(account: Account) {
        router.pushController(RouterTransaction.with(ProfileController.newInstance(account)))
    }

    override fun onPhotoClicked(imageView: ImageView, photoUrl: String) {
        (parentController as? FullScreenPhotoHandler)?.displayFullScreenPhoto(imageView, photoUrl)
    }

    override fun onTootClicked(status: Status) {
        comingSoon()
    }

    private fun observeStream() {
        viewModel.onStreamedResult.observe(this) {
            if (!it.hasBeenHandled) {
                it.getContentIfNotHandled()

                if (recyclerView.isNearTop() && adapterStateRestored) {
                    launch(Dispatchers.Main) {
                        delay(200)
                        containerView ?: return@launch
                        if (recyclerView?.isNearTop() == true) {
                            recyclerView?.smoothScrollToPosition(0)
                        }
                    }
                } else {
                    if (!tootButtonVisible) {
                        showNewTootsIndicator()
                    }
                }
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

        tootButtonVisible = true

        newTootButton.onClick {
            hideNewTootsIndicator()
            recyclerView?.smoothScrollToPosition(0)
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

        tootButtonVisible = false
    }

    private fun restoreAdapterState(savedInstanceState: Bundle) {
        savedInstanceState.getParcelable<Parcelable>(STATE_LAYOUT_MANAGER)?.let { state ->
            (recyclerView.layoutManager as? LinearLayoutManager)?.onRestoreInstanceState(state)
        }
    }

    private fun RecyclerView.isNearTop(): Boolean =
            (layoutManager as LinearLayoutManager?)?.run {
                return@run findFirstVisibleItemPosition() <= 2
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
package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.retention.retained
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.extension.observe
import io.github.jamiesanson.mammut.extension.snackbar
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedModule
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedScope
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.ReselectListener
import kotlinx.android.synthetic.main.fragment_feed.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import me.saket.inboxrecyclerview.executeOnMeasure
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onScrollChange
import javax.inject.Inject

class FeedFragment: Fragment(), ReselectListener {

    private lateinit var viewModel: FeedViewModel

    @Inject
    @FeedScope
    lateinit var factory: MammutViewModelFactory

    // REGION DYNAMIC STATE
    private var adapterStateRestored: Boolean = false
    private var firstSmoothScrollSkipped: Boolean = false
    private var tootButtonVisible: Boolean = false
    // END REGION

    private val feedModule: FeedModule by retained {
        val type: FeedType = arguments?.getParcelable(ARG_TYPE) ?: throw IllegalArgumentException("Missing feed type for feed fragment")
        FeedModule(type)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity() as InstanceActivity)
                .component
                .plus(feedModule)
                .inject(this)

        viewModel = ViewModelProviders.of(this, factory)[FeedViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_feed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.layoutManager = LinearLayoutManager(context).apply {
            initialPrefetchItemCount = 20
        }
        recyclerView.adapter = FeedAdapter(viewModel::loadAround)

        // The following ensures we only change animate the display of the progress bar if
        // showing for the first time, i.e don't transition again after a rotation
        if (savedInstanceState == null) {
            val transition = AutoTransition()
            transition.excludeTarget(recyclerView, true)
            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)
        }

        progressBar.visibility = View.VISIBLE

        newTootButton.executeOnMeasure {
            if (savedInstanceState?.getBoolean(STATE_NEW_TOOTS_VISIBLE) == true) {
                showNewTootsIndicator(animate = false)
            } else {
                hideNewTootsIndicator(animate = false)
            }
        }

        recyclerView.onScrollChange { _,_,_,_,_ ->
            recyclerView ?: return@onScrollChange

            if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE && recyclerView.isNearTop()) {
                if (newTootButton.translationY == 0F) {
                    hideNewTootsIndicator()
                }
            }
        }

        viewModel.errors.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                snackbar(it)
            }
        }

        launch {
            val liveData = viewModel.results.await()
            withContext(UI) {
                onResultsReady(liveData)

                if (!adapterStateRestored) {
                    savedInstanceState?.let(::restoreAdapterState)
                    viewModel.startStreaming()
                }

                observeStream()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = (recyclerView?.layoutManager as? LinearLayoutManager)?.onSaveInstanceState()
        outState.putParcelable(STATE_LAYOUT_MANAGER, state)
        outState.putBoolean(STATE_NEW_TOOTS_VISIBLE, tootButtonVisible)
    }

    override fun onTabReselected() {
        recyclerView?.smoothScrollToPosition(0)
    }

    private fun onResultsReady(resultLiveData: LiveData<PagedList<Status>>) {
        resultLiveData.observe(this@FeedFragment) {
            (recyclerView?.adapter as FeedAdapter?)?.submitList(it)

            if (progressBar.visibility == View.VISIBLE) {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun observeStream() {
        viewModel.onStreamedResult.observe(this) {
            it.getContentIfNotHandled()
            // Only scroll to the top if the first item is completely visible. Note, this is
            // invoked before the streamed item is inserted
            if (recyclerView.isNearTop()) {
                // Due to race conditions in state restoration and async madness, this is often called
                // before [isNearTop] is returning valid results. Due to this, we should skip the first
                // call.
                if (firstSmoothScrollSkipped) {
                    launch(UI) {
                        delay(200)
                        recyclerView?.smoothScrollToPosition(0)
                    }
                } else {
                    firstSmoothScrollSkipped = true
                }
            } else {
                if (!tootButtonVisible) {
                    showNewTootsIndicator()
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
        adapterStateRestored = true
    }

    private fun RecyclerView.isNearTop(): Boolean =
            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() <= 1

    companion object {

        @JvmStatic
        fun newInstance(type: FeedType): FeedFragment =
                FeedFragment().apply {
                    arguments = bundleOf(
                            ARG_TYPE to type
                    )
                }

        private const val ARG_TYPE = "arg_feedback_type"
        private const val STATE_LAYOUT_MANAGER = "state_layout_manager"
        private const val STATE_NEW_TOOTS_VISIBLE = "new_toots_visible"
    }
}
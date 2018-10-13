package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
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
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.jetbrains.anko.bundleOf
import javax.inject.Inject

class FeedFragment: Fragment(), ReselectListener {

    private lateinit var viewModel: FeedViewModel

    @Inject
    @FeedScope
    lateinit var adapter: FeedAdapter

    @Inject
    @FeedScope
    lateinit var factory: MammutViewModelFactory

    private var adapterStateRestored: Boolean = false

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
            initialPrefetchItemCount = 10
        }
        recyclerView.adapter = adapter

        TransitionManager.beginDelayedTransition(view as ViewGroup)
        progressBar.visibility = View.VISIBLE

        // TODO - Base this on network state
        viewModel.startStreaming()

        viewModel.onStreamedResult.observe(this) {
            it.getContentIfNotHandled()
            // Only scroll to the top if the first item is completely visible. Note, this is
            // invoked before the streamed item is inserted
            if ((recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
                launch(UI) {
                    delay(200)
                    recyclerView?.smoothScrollToPosition(0)
                }
            } else {
                // TODO - Show new items indicator
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
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = (recyclerView?.layoutManager as? LinearLayoutManager)?.onSaveInstanceState()
        outState.putParcelable(STATE_LAYOUT_MANAGER, state)
    }

    override fun onTabReselected() {
        recyclerView?.smoothScrollToPosition(0)
    }

    private fun onResultsReady(resultLiveData: LiveData<PagedList<Status>>) {
        resultLiveData.observe(this@FeedFragment) {
            adapter.submitList(it)

            if (progressBar.visibility == View.VISIBLE) {
                TransitionManager.beginDelayedTransition(view as ViewGroup)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun restoreAdapterState(savedInstanceState: Bundle) {
        savedInstanceState.getParcelable<Parcelable>(STATE_LAYOUT_MANAGER)?.let { state ->
            (recyclerView.layoutManager as? LinearLayoutManager)?.onRestoreInstanceState(state)
        }
        adapterStateRestored = true
    }

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
    }
}
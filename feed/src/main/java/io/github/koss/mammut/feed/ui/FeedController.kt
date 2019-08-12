package io.github.koss.mammut.feed.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.base.dagger.scope.FeedScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.navigation.FullScreenPhotoHandler
import io.github.koss.mammut.base.navigation.NavigationHub
import io.github.koss.mammut.base.navigation.ReselectListener
import io.github.koss.mammut.base.util.arg
import io.github.koss.mammut.base.util.comingSoon
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.retained
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.dagger.FeedComponent
import io.github.koss.mammut.feed.dagger.FeedModule
import io.github.koss.mammut.feed.domain.FeedType
import io.github.koss.mammut.feed.presentation.FeedViewModel
import io.github.koss.mammut.feed.ui.list.FeedAdapter
import io.github.koss.mammut.feed.util.TootCallbacks
import io.github.koss.paging.event.PagingRelay
import io.github.koss.paging.network.*
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_feed.*
import javax.inject.Inject

const val ARG_ACCESS_TOKEN = "access_token"
const val ARG_TYPE = "feed_type"

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class FeedController(args: Bundle) : BaseController(args), ReselectListener, TootCallbacks {

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

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (parentController as SubcomponentFactory)
                .buildSubcomponent<FeedModule, FeedComponent>(feedModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context as AppCompatActivity, factory).get(uniqueId, FeedViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_feed, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        super.initialise(savedInstanceState)

        // Only show the progress bar if we're displaying this controller the first time
        if (savedInstanceState == null) {
            // The following ensures we only change animate the display of the progress bar if
            // showing for the first time, i.e don't transition again after a rotation
            val transition = AutoTransition()
            transition.excludeTarget(recyclerView, true)
            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)

            progressBar.visibility = View.VISIBLE
        }

        recyclerView.adapter = FeedAdapter(
                viewModelProvider = ViewModelProviders.of(activity as FragmentActivity, factory),
                tootCallbacks = this,
                pagingRelay = pagingRelay,
                onBrokenTimelineResolved = { TODO() }
        )

        recyclerView.layoutManager = LinearLayoutManager(view!!.context)

        viewModel.feedData.observe(this) {
            (recyclerView.adapter as? FeedAdapter)?.submitList(it)
        }

        viewModel.loadingState.observe(this, ::processLoadingState)
    }

    override fun onTabReselected() {
        recyclerView?.smoothScrollToPosition(0)
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

    private fun processLoadingState(loadingState: LoadingState) {
        when (loadingState) {
            LoadingAtEnd -> {
                bottomLoadingIndicator.isVisible = true
            }
            LoadingAtFront -> {
                topLoadingIndicator.isVisible = true
            }
            LoadingAll -> {
                progressBar.isVisible = true
            }
            NotLoading -> {
                progressBar.isVisible = false
                bottomLoadingIndicator.isVisible = false
                topLoadingIndicator.isVisible = false
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(type: FeedType): FeedController =
                FeedController(bundleOf(
                        ARG_TYPE to type
                ))
    }
}
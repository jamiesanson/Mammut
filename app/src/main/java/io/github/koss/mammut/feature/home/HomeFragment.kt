package io.github.koss.mammut.feature.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.R
import io.github.koss.mammut.base.dagger.scope.ApplicationScope
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.navigation.Event
import io.github.koss.mammut.base.navigation.Tab
import io.github.koss.mammut.base.util.*
import io.github.koss.mammut.base.widget.HideTopViewOnScrollBehavior
import io.github.koss.mammut.component.widget.InstanceBottomNavigationView
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.databinding.HomeFragmentBinding
import io.github.koss.mammut.feature.home.dagger.HomeComponent
import io.github.koss.mammut.feature.home.dagger.HomeModule
import io.github.koss.mammut.feature.home.presentation.HomeViewModel
import io.github.koss.mammut.feature.home.presentation.navigation.NavigationEvent
import io.github.koss.mammut.feature.home.presentation.navigation.UserPeekRequested
import io.github.koss.mammut.feature.home.presentation.state.HomeState
import io.github.koss.mammut.feature.home.view.bindFeedTypeButton
import io.github.koss.mammut.feature.home.view.closeChooser
import io.github.koss.mammut.feature.home.view.openChooser
import io.github.koss.mammut.feature.home.view.setupChooser
import io.github.koss.mammut.feature.joininstance.JoinInstanceActivity
import io.github.koss.mammut.base.photoviewer.FullScreenPhotoDelegate
import io.github.koss.mammut.base.photoviewer.FullScreenPhotoViewer
import io.github.koss.mammut.feature.profile.ProfileFragmentArgs
import io.github.koss.mammut.feed.presentation.FeedTypeProvider
import io.github.koss.mammut.repo.RegistrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeFragment: Fragment(R.layout.home_fragment), FeedTypeProvider, FullScreenPhotoViewer by FullScreenPhotoDelegate() {

    private val binding by viewLifecycleLazy { HomeFragmentBinding.bind(requireView()) }

    @Inject
    @InstanceScope
    lateinit var viewModelFactory: MammutViewModelFactory

    @Inject
    @ApplicationScope
    lateinit var registrationRepository: RegistrationRepository

    private lateinit var homeViewModel: HomeViewModel

    override val currentFeedType: FeedType?
        get() = if (::homeViewModel.isInitialized) homeViewModel.state.value?.selectedFeedType else null

    private val component by lazy {
        findSubcomponentFactory()
                .buildSubcomponent<HomeModule, HomeComponent>(HomeModule)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)

        homeViewModel = ViewModelProvider(activity as AppCompatActivity, viewModelFactory)
                .get(component.accessToken(), HomeViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNavigation()
        setupView()

        setPhotoTargetViewBinding {
            binding.fullScreenPhotoLayout to binding.fullScreenGestureImageView
        }

        homeViewModel.state.observe(viewLifecycleOwner, ::onStateChanged)
        homeViewModel.navigationEvents.observe(viewLifecycleOwner, ::onNavigationEvent)
    }

    private fun setupView() {
        binding.bottomSheet.initialise()

        binding.feedTypeButton.setOnClickListener {
            binding.openChooser()
        }

        binding.feedTypeDim.setOnClickListener {
            binding.closeChooser()
        }

        binding.bottomNavigationDim.setOnClickListener {
            binding.bottomSheet.collapse()
        }

        binding.feedChooserCardContent.doOnApplyWindowInsets { view, insets, initialState ->
            view.updatePadding(top = initialState.paddings.top + insets.systemWindowInsetTop)
        }

        binding.addButton.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToComposeTootFragment(
                    accessToken = component.accessToken(),
                    instanceName = component.instanceName(),
                    account = homeViewModel.state.value?.currentUser!!
            ))
        }

        hideFeedIndicatorDelayed()
    }

    private fun setupNavigation() {
        val instanceController = childFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
        NavigationUI.setupWithNavController(binding.bottomSheet.navigationView, instanceController)

        binding.bottomSheet.navigationView.setOnNavigationItemReselectedListener { item ->
            if (item.itemId == instanceController.currentBackStackEntry?.destination?.id) {
                val tab = when (item.itemId) {
                    R.id.feed -> Tab.Feed
                    R.id.search -> Tab.Search
                    R.id.notifications -> Tab.Notifications
                    else -> throw IllegalArgumentException("Unknown menu item $item")
                }

                homeViewModel.reselectTab(tab)
            }
        }

        instanceController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.feed -> {
                    binding.bottomSheet.collapse()
                    binding.feedTypeButton.isVisible = true
                }
                else -> {
                    binding.bottomSheet.collapse()
                    binding.feedTypeButton.isVisible = false
                    binding.feedChooserCard.isVisible = false
                }
            }
        }


        with(binding.bottomSheet) {
            onNavigationClickListener = object : InstanceBottomNavigationView.OnNavigationClickListener {
                override fun onNavigationClicked(destination: InstanceBottomNavigationView.NavigationDestination) =
                        onSheetNavigationItemClicked(destination)
            }

            onSheetScrollListener = object : InstanceBottomNavigationView.OnSheetScrollListener {
                override fun onScrolled(proportion: Float) {
                    if (view == null) return // Sometimes this is dispatches after we've navigated away
                    when (proportion) {
                        0f -> {
                            binding.bottomNavigationDim.isVisible = false
                        }
                        else -> binding.bottomNavigationDim.apply {
                            isVisible = true
                            alpha = proportion
                        }
                    }
                }
            }

            onInstanceChangeListener = object : InstanceBottomNavigationView.OnInstanceChangeListener {
                override fun onInstanceChanged(index: Int) = onInstanceIndexSelected(index)
            }
        }

    }

    private fun onStateChanged(state: HomeState) {
        // Update the bottom sheet
        binding.bottomSheet.setState(state)

        binding.bottomSheet.navigationView.menu.forEach {
            if (it.itemId == R.id.feed) {
                when (state.selectedFeedType) {
                    FeedType.Home -> it.title = getString(R.string.home_feed_label)
                    FeedType.Local -> it.title = getString(R.string.local_feed_label)
                    FeedType.Federated -> it.title = getString(R.string.federated_feed_label)
                }
            }
        }

        // Configure feed type button
        binding.bindFeedTypeButton(state.selectedFeedType)

        // Setup chooser
        binding.setupChooser(selectedFeedType = state.selectedFeedType) {
            homeViewModel.changeFeedType(it)

            // Hide the feed chooser
            hideFeedIndicatorDelayed()
        }

        if (state.offscreenItemCount == 0) {
            binding.bottomSheet.navigationView.removeBadge(R.id.feed)
        } else {
            binding.bottomSheet.navigationView.getOrCreateBadge(R.id.feed)
        }
    }

    private fun onNavigationEvent(event: Event<NavigationEvent>) {
        when (event.getContentIfNotHandled()) {
            null -> return
            UserPeekRequested -> binding.bottomSheet.peekCurrentUser()
        }
    }

    private fun onSheetNavigationItemClicked(destination: InstanceBottomNavigationView.NavigationDestination) {
        when (destination) {
            InstanceBottomNavigationView.NavigationDestination.Settings -> {
                findRootNavController().navigate(HomeFragmentDirections.actionHomeFragmentToSettingsFragment())
            }
            InstanceBottomNavigationView.NavigationDestination.PendingWork -> {
                findRootNavController().navigate(HomeFragmentDirections.actionHomeFragmentToPendingWorkFragment())
            }
            InstanceBottomNavigationView.NavigationDestination.AboutApp -> {
                findRootNavController().navigate(HomeFragmentDirections.actionHomeFragmentToAboutAppFragment())
            }
            InstanceBottomNavigationView.NavigationDestination.Profile -> {
                val parameters = binding.feedTypeButton.layoutParams as CoordinatorLayout.LayoutParams
                val behavior = parameters.behavior as HideTopViewOnScrollBehavior<View>
                behavior.hideView(binding.feedTypeButton)

                findRootNavController().navigate(R.id.profileFragment, ProfileFragmentArgs(isMe = true).toBundle())
            }
            InstanceBottomNavigationView.NavigationDestination.JoinInstance -> {
                startActivity(Intent(context, JoinInstanceActivity::class.java))
            }
        }
    }

    private fun onInstanceIndexSelected(index: Int) {
        findNavigationHub().switchInstance(index)
    }

    private fun hideFeedIndicatorDelayed() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            delay(5000L)

            // Hide the view if not already
            val parameters = binding.feedTypeButton.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = parameters.behavior as HideTopViewOnScrollBehavior<View>
            behavior.hideView(binding.feedTypeButton)
        }
    }
}
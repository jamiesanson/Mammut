package io.github.koss.mammut.feature.instance

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
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
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.base.dagger.scope.ApplicationScope
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.navigation.Event
import io.github.koss.mammut.base.navigation.Tab
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.retained
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.component.widget.HideTopViewOnScrollBehavior
import io.github.koss.mammut.component.widget.InstanceBottomNavigationView
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.databinding.InstanceFragmentTwoBinding
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.multiinstance.MultiInstanceFragment
import io.github.koss.mammut.feature.instance.dagger.InstanceComponent
import io.github.koss.mammut.feature.instance.dagger.InstanceModule
import io.github.koss.mammut.feature.instance.presentation.InstanceViewModel
import io.github.koss.mammut.feature.instance.presentation.navigation.NavigationEvent
import io.github.koss.mammut.feature.instance.presentation.navigation.UserPeekRequested
import io.github.koss.mammut.feature.instance.presentation.state.InstanceState
import io.github.koss.mammut.feature.instance.view.*
import io.github.koss.mammut.feature.joininstance.JoinInstanceActivity
import io.github.koss.mammut.feature.multiinstance.MultiInstanceFragmentDirections
import io.github.koss.mammut.feature.profile.ProfileFragmentArgs
import io.github.koss.mammut.feature.profile.dagger.ProfileModule
import io.github.koss.mammut.feed.dagger.FeedModule
import io.github.koss.mammut.feed.presentation.FeedTypeProvider
import io.github.koss.mammut.notifications.dagger.NotificationsModule
import io.github.koss.mammut.repo.RegistrationRepository
import io.github.koss.mammut.toot.dagger.ComposeTootModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.koss.mammut.feature.multiinstance.MultiInstanceFragmentDirections.Companion.actionMultiInstanceFragmentToAboutAppFragment as toAboutAppFragment
import io.github.koss.mammut.feature.multiinstance.MultiInstanceFragmentDirections.Companion.actionMultiInstanceFragmentToPendingWorkFragment as toPendingWorkFragment
import io.github.koss.mammut.feature.multiinstance.MultiInstanceFragmentDirections.Companion.actionMultiInstanceFragmentToSettingsFragment as toSettingsFragment

const val ARG_INSTANCE_NAME = "arg_instance_name"
const val ARG_AUTH_CODE = "arg_auth_code"

class InstanceFragment : Fragment(R.layout.instance_fragment_two), SubcomponentFactory, FeedTypeProvider {

    private val binding by viewLifecycleLazy { InstanceFragmentTwoBinding.bind(requireView()) }

    private val component: InstanceComponent by retained(key = { requireArguments().getString(ARG_AUTH_CODE)!! }) {
        (context as AppCompatActivity).applicationComponent
                .plus(instanceModule)
    }

    private val instanceModule: InstanceModule by lazy {
        InstanceModule(
                instanceName = requireArguments().getString(ARG_INSTANCE_NAME)!!,
                accessToken = requireArguments().getString(ARG_AUTH_CODE)!!)
    }

    @Inject
    @InstanceScope
    lateinit var viewModelFactory: MammutViewModelFactory

    @Inject
    @ApplicationScope
    lateinit var registrationRepository: RegistrationRepository

    private lateinit var instanceViewModel: InstanceViewModel

    override val currentFeedType: FeedType?
        get() = if (::instanceViewModel.isInitialized) instanceViewModel.state.value?.selectedFeedType else null

    private val instanceController
        get() = childFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)

        instanceViewModel = ViewModelProvider(activity as AppCompatActivity, viewModelFactory)
                .get(component.accessToken(), InstanceViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNavigation()
        setupView()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!instanceController.popBackStack()) {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        instanceViewModel.state.observe(viewLifecycleOwner, ::onStateChanged)
        instanceViewModel.navigationEvents.observe(viewLifecycleOwner, ::onNavigationEvent)
    }

    private fun setupView() {
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
            findNavController().navigate(MultiInstanceFragmentDirections.actionMultiInstanceFragmentToComposeTootFragment(
                    accessToken = component.accessToken(),
                    instanceName = component.instanceName(),
                    account = instanceViewModel.state.value?.currentUser!!
            ))
        }

        hideFeedIndicatorDelayed()
    }

    private fun setupNavigation() {
        NavigationUI.setupWithNavController(binding.bottomSheet.navigationView, instanceController)

        binding.bottomSheet.navigationView.setOnNavigationItemReselectedListener { item ->
            if (item.itemId == instanceController.currentBackStackEntry?.destination?.id) {
                val tab = when (item.itemId) {
                    R.id.feed -> Tab.Feed
                    R.id.search -> Tab.Search
                    R.id.notifications -> Tab.Notifications
                    else -> throw IllegalArgumentException("Unknown menu item $item")
                }

                instanceViewModel.reselectTab(tab)
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
                    when (proportion) {
                        0f -> {
                            binding.bottomNavigationDim.isVisible = false
                            (parentFragment as MultiInstanceFragment).unlockViewPager()
                        }
                        else -> binding.bottomNavigationDim.apply {
                            isVisible = true
                            alpha = proportion
                            (parentFragment as MultiInstanceFragment).lockViewPager()
                        }
                    }
                }
            }

            onInstanceChangeListener = object : InstanceBottomNavigationView.OnInstanceChangeListener {
                override fun onInstanceChanged(index: Int) = onInstanceIndexSelected(index)
            }
        }

    }

    private fun onStateChanged(state: InstanceState) {
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
            instanceViewModel.changeFeedType(it)

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
        val parentNavController = parentFragment?.findNavController()!!
        when (destination) {
            InstanceBottomNavigationView.NavigationDestination.Settings -> {
                parentNavController.navigate(toSettingsFragment())
            }
            InstanceBottomNavigationView.NavigationDestination.PendingWork -> {
                parentNavController.navigate(toPendingWorkFragment())
            }
            InstanceBottomNavigationView.NavigationDestination.AboutApp -> {
                parentNavController.navigate(toAboutAppFragment())
            }
            InstanceBottomNavigationView.NavigationDestination.Profile -> {
                val parameters = binding.feedTypeButton.layoutParams as CoordinatorLayout.LayoutParams
                val behavior = parameters.behavior as HideTopViewOnScrollBehavior<View>
                behavior.hideView(binding.feedTypeButton)

                instanceController.navigate(R.id.profileFragment, ProfileFragmentArgs(isMe = true).toBundle())
            }
            InstanceBottomNavigationView.NavigationDestination.JoinInstance -> {
                startActivity(Intent(context, JoinInstanceActivity::class.java))
            }
        }
    }

    private fun onInstanceIndexSelected(index: Int) {
        (parentFragment as MultiInstanceFragment).requestPageSelection(index)
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

    override fun <Module, Subcomponent> buildSubcomponent(module: Module): Subcomponent {
        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
        return when (module) {
            is ComposeTootModule -> component.plus(module)
            is NotificationsModule -> component.plus(module)
            is FeedModule -> component.plus(module)
            is ProfileModule -> component.plus(module)
            else -> throw IllegalArgumentException("Unknown module type")
        } as Subcomponent
    }
}
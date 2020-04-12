package io.github.koss.mammut.feature.instance2

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import io.github.koss.mammut.R
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.util.*
import io.github.koss.mammut.component.widget.InstanceBottomNavigationView
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.databinding.InstanceFragmentTwoBinding
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.instance.MultiInstanceFragment
import io.github.koss.mammut.feature.instance.bottomnav.BottomNavigationViewModel
import io.github.koss.mammut.feature.instance.dagger.InstanceComponent
import io.github.koss.mammut.feature.instance.dagger.InstanceModule
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_AUTH_CODE
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_INSTANCE_NAME
import io.github.koss.mammut.repo.RegistrationRepository
import javax.inject.Inject

class InstanceFragment : Fragment(R.layout.instance_fragment_two) {

    private val binding by viewLifecycleLazy { InstanceFragmentTwoBinding.bind(requireView()) }

    lateinit var component: InstanceComponent

    private val instanceModule: InstanceModule by retained(key = { requireArguments().getString(ARG_AUTH_CODE)!! }) {
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

    private lateinit var bottomNavigationViewModel: BottomNavigationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = (context as AppCompatActivity).applicationComponent
                .plus(instanceModule)

        component.inject(this)

        bottomNavigationViewModel = ViewModelProvider(activity as AppCompatActivity, viewModelFactory)
                .get(component.accessToken(), BottomNavigationViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navController = childFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
        NavigationUI.setupWithNavController(binding.bottomSheet.navigationView, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.home,
                    R.id.localTimeline,
                    R.id.federatedTimeline,
                    R.id.notifications -> binding.bottomSheet.collapse()
            }
        }

        bottomNavigationViewModel.viewState.observe(viewLifecycleOwner) {
            binding.bottomSheet.setState(it)
        }

        with (binding.bottomSheet) {
            onNavigationClickListener = object: InstanceBottomNavigationView.OnNavigationClickListener {
                override fun onNavigationClicked(destination: InstanceBottomNavigationView.NavigationDestination) =
                        onSheetNavigationItemClicked(destination)
            }

            onSheetScrollListener = object: InstanceBottomNavigationView.OnSheetScrollListener {
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

            onInstanceChangeListener = object: InstanceBottomNavigationView.OnInstanceChangeListener {
                override fun onInstanceChanged(index: Int) = onInstanceIndexSelected(index)
            }
        }

        binding.addButton.setOnClickListener {
            binding.bottomSheet.expand()
        }
    }

    private fun onSheetNavigationItemClicked(destination: InstanceBottomNavigationView.NavigationDestination) {
        when (destination) {
            InstanceBottomNavigationView.NavigationDestination.Settings,
            InstanceBottomNavigationView.NavigationDestination.PendingWork,
            InstanceBottomNavigationView.NavigationDestination.AboutApp,
            InstanceBottomNavigationView.NavigationDestination.JoinInstance,
            InstanceBottomNavigationView.NavigationDestination.Profile -> {} // TODO - implement navigation
        }
    }

    private fun onInstanceIndexSelected(index: Int) {
        (parentFragment as MultiInstanceFragment).requestPageSelection(index)
    }

    fun peekCurrentUser() {
        binding.bottomSheet.peekCurrentUser()
    }
}
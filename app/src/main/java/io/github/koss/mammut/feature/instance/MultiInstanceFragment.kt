package io.github.koss.mammut.feature.instance

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.github.koss.mammut.R
import io.github.koss.mammut.base.util.awaitFirst
import io.github.koss.mammut.base.util.viewLifecycleLazy
import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.databinding.MultiInstanceFragmentBinding
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_AUTH_CODE
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_INSTANCE_NAME
import io.github.koss.mammut.feature.instance2.InstanceFragment
import io.github.koss.mammut.repo.PreferencesRepository
import io.github.koss.mammut.repo.RegistrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class MultiInstanceFragment: Fragment(R.layout.multi_instance_fragment) {

    @Inject
    lateinit var registrationRepository: RegistrationRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val binding by viewLifecycleLazy { MultiInstanceFragmentBinding.bind(requireView()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (context as AppCompatActivity).applicationComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.isUserInputEnabled = preferencesRepository.swipingBetweenInstancesEnabled

        // Setup registrations
        observeRegistrations()
    }

    fun lockViewPager() {
        binding.viewPager.isUserInputEnabled = false
    }

    fun unlockViewPager() {

        // Only properly unlock the ViewPager if enabled.
        if (preferencesRepository.swipingBetweenInstancesEnabled) {
            binding.viewPager.isUserInputEnabled = true
        }
    }

    fun requestPageSelection(index: Int) {
        binding.viewPager.setCurrentItem(index, true)
    }

    fun onBackPressed(): Boolean = findNavController().popBackStack()

    private fun observeRegistrations() {
        val liveData = registrationRepository.getAllCompletedRegistrationsLive()

        // Observe registrations with the view lifecycle
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            liveData.awaitFirst().let(::setupPager)
            liveData.observe(viewLifecycleOwner, Observer {
                (binding.viewPager.adapter as? InstancePagerAdapter)?.registrations = it
            })
        }
    }

    private fun setupPager(registrations: List<InstanceRegistration>) {
        val pagerAdapter = InstancePagerAdapter(this).apply { this.registrations = registrations }

        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentFragment = childFragmentManager.fragments
                        .find { it.isResumed } as? InstanceFragment

                currentFragment?.peekCurrentUser()
            }
        })
    }

    @SuppressLint("WrongConstant")
    private class InstancePagerAdapter(hostFragment: Fragment) : FragmentStateAdapter(hostFragment.childFragmentManager, hostFragment.lifecycle) {

        var registrations: List<InstanceRegistration> = emptyList()
            set(value) {
                if (field != value) {
                    field = value
                    notifyDataSetChanged()
                }
            }

        override fun getItemCount(): Int = registrations.size

        override fun createFragment(position: Int): Fragment = InstanceFragment().apply {
            arguments = bundleOf(
                    ARG_AUTH_CODE to registrations[position].accessToken?.accessToken,
                    ARG_INSTANCE_NAME to registrations[position].instanceName
            )
        }

        override fun getItemId(position: Int): Long = registrations[position].id
    }
}
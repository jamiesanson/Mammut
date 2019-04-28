package io.github.koss.mammut.feature.instance

import android.content.Context
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.extension.awaitFirst
import io.github.koss.mammut.extension.observe
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_AUTH_CODE
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_INSTANCE_NAME
import io.github.koss.mammut.feature.instance.subfeature.navigation.InstanceController
import io.github.koss.mammut.repo.PreferencesRepository
import io.github.koss.mammut.repo.RegistrationRepository
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_multi_instance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.dip
import org.jetbrains.anko.support.v4.onPageChangeListener
import javax.inject.Inject

@ContainerOptions(CacheImplementation.NO_CACHE)
class MultiInstanceController : BaseController() {

    @Inject
    lateinit var registrationRepository: RegistrationRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as AppCompatActivity).applicationComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_multi_instance, container, false)

    override fun onAttach(view: View) {
        super.onAttach(view)

        viewPager.swipeLocked = !preferencesRepository.swipingBetweenInstancesEnabled

        val liveData = registrationRepository.getAllCompletedRegistrationsLive()
        launch(Dispatchers.Main) {
            liveData.awaitFirst().let(::setupPager)
            liveData.observe(this@MultiInstanceController) {
                (viewPager.adapter as? InstancePagerAdapter)?.registrations = it
            }
        }
    }

    fun lockViewPager() {
        containerView ?: return
        viewPager.swipeLocked = true
    }

    fun unlockViewPager() {
        containerView ?: return

        // Only properly unlock the ViewPager if enabled.
        if (preferencesRepository.swipingBetweenInstancesEnabled) {
            viewPager.swipeLocked = false
        }
    }

    fun requestPageSelection(index: Int) {
        viewPager.setCurrentItem(index, true)
    }

    private fun setupPager(registrations: List<InstanceRegistration>) {
        if (childRouters.isNotEmpty() && (viewPager.adapter as InstancePagerAdapter?)?.registrations == registrations) return

        val pagerAdapter = InstancePagerAdapter(this).apply { this.registrations = registrations }

        viewPager.adapter = pagerAdapter
        viewPager.pageMargin = viewPager.context.dip(32)

        viewPager.clearOnPageChangeListeners()

        viewPager.onPageChangeListener {
            onPageSelected { index ->
                val router = pagerAdapter.getRouter(index) ?: return@onPageSelected

                if (router.backstackSize == 1) {
                    (router.backstack[0].controller() as? InstanceController?)?.peekCurrentUser()
                }
            }
        }
    }

    private class InstancePagerAdapter(hostController: Controller) : RouterPagerAdapter(hostController) {

        var registrations: List<InstanceRegistration> = emptyList()
            set(value) {
                if (field != value) {
                    field = value
                    notifyDataSetChanged()
                }
            }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {

                val controller = InstanceController(args = bundleOf(
                        ARG_AUTH_CODE to registrations[position].accessToken?.accessToken,
                        ARG_INSTANCE_NAME to registrations[position].instanceName))

                controller.retainViewMode = Controller.RetainViewMode.RETAIN_DETACH

                router.setRoot(RouterTransaction
                        .with(controller))
            }
        }

        override fun getCount(): Int = registrations.size

        override fun getItemId(position: Int): Long = registrations[position].id
    }
}
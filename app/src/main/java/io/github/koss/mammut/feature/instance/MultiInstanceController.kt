package io.github.koss.mammut.feature.instance

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_AUTH_CODE
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_INSTANCE_NAME
import io.github.koss.mammut.feature.instance.subfeature.navigation.InstanceController
import io.github.koss.mammut.repo.RegistrationRepository
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_multi_instance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.dip
import javax.inject.Inject

@ContainerOptions(CacheImplementation.NO_CACHE)
class MultiInstanceController: BaseController() {

    @Inject
    lateinit var registrationRepository: RegistrationRepository

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as AppCompatActivity).applicationComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_multi_instance, container, false)

    override fun onAttach(view: View) {
        super.onAttach(view)
        launch {
            val regos = registrationRepository.getAllRegistrations().filterNot { it.accessToken == null }
            withContext(Dispatchers.Main) {
                setupPager(regos)
            }
         }
    }

    private fun setupPager(registrations: List<InstanceRegistration>) {
        val pagerAdapter = object : RouterPagerAdapter(this) {

            override fun configureRouter(router: Router, position: Int) {
                if (!router.hasRootController()) {
                    val controller = InstanceController(args = bundleOf(
                            ARG_AUTH_CODE to registrations[position].accessToken?.accessToken,
                            ARG_INSTANCE_NAME to registrations[position].instanceName))

                    router.setRoot(RouterTransaction
                            .with(controller))
                }
            }

            override fun getCount(): Int = registrations.size
        }

        viewPager.adapter = pagerAdapter
        viewPager.pageMargin = viewPager.context.dip(32)
    }

}
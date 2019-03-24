package io.github.koss.mammut.feature.instance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseActivity
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.repo.PreferencesRepository
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.instance.dagger.InstanceComponent
import io.github.koss.mammut.feature.instance.dagger.InstanceModule
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_AUTH_CODE
import io.github.koss.mammut.feature.instance.subfeature.navigation.ARG_INSTANCE_NAME
import io.github.koss.mammut.feature.instance.subfeature.navigation.InstanceController
import io.github.koss.mammut.toot.dagger.ComposeTootModule
import kotlinx.android.synthetic.main.activity_instance.*
import javax.inject.Inject

private const val EXTRA_INSTANCE_NAME = "instance_name"
private const val EXTRA_AUTH_CODE = "auth_code"

/**
 * Main fragment for hosting an instance. Handles navigation within the instance.
 */
class InstanceActivity : BaseActivity(), SubcomponentFactory {


    lateinit var component: InstanceComponent

    private lateinit var router: Router

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instance)

        router = Conductor.attachRouter(this, baseLayout, savedInstanceState)
        if (!router.hasRootController()) {
            val instanceName: String = intent?.extras?.getString(EXTRA_INSTANCE_NAME)
                    ?: throw NullPointerException("Instance name must not be null")
            val authCode: String = intent?.extras?.getString(EXTRA_AUTH_CODE)
                    ?: throw NullPointerException("Auth code must not be null")

            router.setRoot(RouterTransaction.with(InstanceController(bundleOf(
                    ARG_AUTH_CODE to authCode,
                    ARG_INSTANCE_NAME to instanceName))))
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

    override fun injectDependencies() {
        val instanceName: String = intent?.extras?.getString(EXTRA_INSTANCE_NAME)
                ?: throw NullPointerException("Instance name must not be null")
        val authCode: String = intent?.extras?.getString(EXTRA_AUTH_CODE)
                ?: throw NullPointerException("Auth code must not be null")

        component = applicationComponent
                .plus(InstanceModule(instanceName, authCode)).also {
                    it.inject(this)
                }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Module, Subcomponent> buildSubcomponent(module: Module): Subcomponent {
        return when (module) {
            is ComposeTootModule -> component.plus(module)
            else -> throw IllegalArgumentException("Unknown module type")
        } as Subcomponent
    }

    override fun onStop() {
        super.onStop()
        // Save the current instance to shared preferences
        preferencesRepository.lastAccessedInstanceToken = intent?.extras?.getString(EXTRA_AUTH_CODE)
                ?: throw NullPointerException("Auth code must not be null")
    }

    companion object {

        @JvmStatic
        fun launch(context: Context, instanceName: String, authCode: String) {
            Intent(context, InstanceActivity::class.java)
                    .apply {
                        putExtra(EXTRA_INSTANCE_NAME, instanceName)
                        putExtra(EXTRA_AUTH_CODE, authCode)
                    }.run {
                        context.startActivity(this)
                    }
        }
    }
}
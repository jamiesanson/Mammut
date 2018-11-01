package io.github.jamiesanson.mammut.feature.instance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.extension.applicationComponent
import io.github.jamiesanson.mammut.feature.base.BaseActivity
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceComponent
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceModule
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.InstanceController
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.ReselectListener
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.Tab
import kotlinx.android.synthetic.main.activity_instance.*
import org.jetbrains.anko.contentView

private const val EXTRA_INSTANCE_NAME = "instance_name"
private const val EXTRA_AUTH_CODE = "auth_code"

/**
 * Main fragment for hosting an instance. Handles navigation within the instance.
 */
class InstanceActivity : BaseActivity() {

    lateinit var component: InstanceComponent

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instance)

        router = Conductor.attachRouter(this, baseLayout, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(InstanceController()))
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

        private const val STATE_CURRENT_TAB = "current_tab"
    }
}
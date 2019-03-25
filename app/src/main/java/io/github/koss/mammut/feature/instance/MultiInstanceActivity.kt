package io.github.koss.mammut.feature.instance

import android.os.Bundle
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseActivity
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.repo.PreferencesRepository
import kotlinx.android.synthetic.main.activity_multi_instance.*
import javax.inject.Inject

class MultiInstanceActivity: BaseActivity() {

    private lateinit var router: Router

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun injectDependencies() {
        applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_instance)

        router = Conductor.attachRouter(this, parentLayout, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(MultiInstanceController()))
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }
}
package io.github.koss.mammut.feature.instance

import android.os.Bundle
import androidx.fragment.app.findFragment
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseActivity
import io.github.koss.mammut.databinding.ActivityMultiInstanceTwoBinding
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.repo.PreferencesRepository
import kotlinx.android.synthetic.main.activity_multi_instance.*
import javax.inject.Inject

class MultiInstanceActivity: BaseActivity() {

    private var router: Router? = null

    private var binding: ActivityMultiInstanceTwoBinding? = null

    private val useConductor = false

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun injectDependencies() {
        applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (useConductor) {
            setContentView(R.layout.activity_multi_instance)
            setupConductor(savedInstanceState)
        } else {
            binding = ActivityMultiInstanceTwoBinding.inflate(layoutInflater)
            setContentView(binding!!.root)
        }
    }

    private fun setupConductor(savedInstanceState: Bundle?) {
        router = Conductor.attachRouter(this, parentLayout, savedInstanceState)
        if (!router!!.hasRootController()) {
            router?.setRoot(RouterTransaction.with(MultiInstanceController()))
        }
    }

    override fun onBackPressed() {
        if (router?.handleBack() == false || binding?.multiInstanceFragment?.findFragment<MultiInstanceFragment>()?.onBackPressed() == false) {
            super.onBackPressed()
        }
    }
}
package io.github.koss.mammut.feature.multiinstance

import android.os.Bundle
import androidx.navigation.findNavController
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseActivity
import io.github.koss.mammut.databinding.ActivityMultiInstanceBinding
import io.github.koss.mammut.extension.applicationComponent

class MultiInstanceActivity: BaseActivity() {

    override fun injectDependencies() {
        applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMultiInstanceBinding.inflate(layoutInflater).root)
    }

    override fun onBackPressed() {
        if (onBackPressedDispatcher.hasEnabledCallbacks()) {
            super.onBackPressed()
        } else if (!findNavController(R.id.navHostFragment).popBackStack()) {
            super.onBackPressed()
        }
    }
}
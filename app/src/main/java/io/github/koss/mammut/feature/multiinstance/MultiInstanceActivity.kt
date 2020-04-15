package io.github.koss.mammut.feature.multiinstance

import android.os.Bundle
import androidx.navigation.findNavController
import io.github.koss.mammut.R
import io.github.koss.mammut.base.BaseActivity
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.databinding.ActivityMultiInstanceBinding
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.toot.dagger.ComposeTootModule

class MultiInstanceActivity: BaseActivity(), SubcomponentFactory {

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

    override fun <Module, Subcomponent> buildSubcomponent(module: Module): Subcomponent {
        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
        return when (module) {
            is ComposeTootModule -> applicationComponent.plus(module)
            else -> throw IllegalArgumentException("Unknown module type")
        } as Subcomponent
    }
}
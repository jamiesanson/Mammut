package io.github.jamiesanson.mammut.feature.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.retention.retained
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.dagger.application.ApplicationScope
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.BaseController
import io.github.jamiesanson.mammut.feature.settings.dagger.SettingsModule
import io.github.jamiesanson.mammut.feature.settings.dagger.SettingsScope
import io.github.jamiesanson.mammut.feature.themes.ThemeEngine
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_settings.*
import javax.inject.Inject

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class SettingsController: BaseController() {

    private lateinit var viewModel: SettingsViewModel

    @Inject
    @SettingsScope
    lateinit var viewModelFactory: MammutViewModelFactory

    @Inject
    @ApplicationScope
    lateinit var themeEngine: ThemeEngine

    private val settingsModule: SettingsModule by retained {
        SettingsModule()
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as InstanceActivity)
                .component
                .plus(settingsModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context, viewModelFactory).get(SettingsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.controller_settings, container, false)

    override fun initialise(savedInstanceState: Bundle?) {
        super.initialise(savedInstanceState)
        themeEngine.applyFontToCollapsingLayout(collapsingLayout)
    }
}
package io.github.jamiesanson.mammut.feature.settings.dagger

import dagger.Subcomponent
import io.github.jamiesanson.mammut.feature.settings.SettingsController

@SettingsScope
@Subcomponent(modules = [SettingsModule::class])
interface SettingsComponent {

    fun inject(controller: SettingsController)
}
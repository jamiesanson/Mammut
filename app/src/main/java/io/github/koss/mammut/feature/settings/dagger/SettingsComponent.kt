package io.github.koss.mammut.feature.settings.dagger

import dagger.Subcomponent
import io.github.koss.mammut.feature.settings.SettingsFragment

@SettingsScope
@Subcomponent(modules = [SettingsModule::class])
interface SettingsComponent {

    fun inject(controller: SettingsFragment)
}
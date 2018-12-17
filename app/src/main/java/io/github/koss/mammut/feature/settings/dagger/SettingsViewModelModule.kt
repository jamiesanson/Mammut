package io.github.koss.mammut.feature.settings.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.dagger.MammutViewModelFactory
import io.github.koss.mammut.dagger.ViewModelKey
import io.github.koss.mammut.feature.settings.SettingsViewModel

@Module
abstract class SettingsViewModelModule {

    @Binds
    @IntoMap
    @SettingsScope
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @SettingsScope
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
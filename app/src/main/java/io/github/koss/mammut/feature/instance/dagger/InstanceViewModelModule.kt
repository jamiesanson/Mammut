package io.github.koss.mammut.feature.instance.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.viewmodel.ViewModelKey
import io.github.koss.mammut.feature.instance2.presentation.InstanceViewModel

@Module
abstract class InstanceViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(InstanceViewModel::class)
    abstract fun bindBottomNavigationViewModel(viewModel: InstanceViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
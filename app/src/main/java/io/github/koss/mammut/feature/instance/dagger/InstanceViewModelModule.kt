package io.github.koss.mammut.feature.instance.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.viewmodel.ViewModelKey
import io.github.koss.mammut.feature.instance.bottomnav.BottomNavigationViewModel

@Module
abstract class InstanceViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(BottomNavigationViewModel::class)
    abstract fun bindBottomNavigationViewModel(viewModel: BottomNavigationViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
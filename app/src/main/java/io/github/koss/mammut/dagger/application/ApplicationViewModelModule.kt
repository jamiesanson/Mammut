package io.github.koss.mammut.dagger.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.ViewModelKey
import io.github.koss.mammut.feature.instancebrowser.recyclerview.InstanceCardViewModel

@Module
abstract class ApplicationViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(InstanceCardViewModel::class)
    abstract fun bindInstanceCardViewModel(viewModel: InstanceCardViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
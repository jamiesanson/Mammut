package io.github.jamiesanson.mammut.dagger.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.dagger.ViewModelKey
import io.github.jamiesanson.mammut.feature.instancebrowser.recyclerview.InstanceCardViewModel

@Module
abstract class ApplicationViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(InstanceCardViewModel::class)
    abstract fun bindInstanceCardViewModel(viewModel: InstanceCardViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
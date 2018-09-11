package io.github.jamiesanson.mammut.feature.joininstance.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.dagger.ViewModelKey
import io.github.jamiesanson.mammut.feature.joininstance.JoinInstanceViewModel

@Module
abstract class JoinInstanceViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(JoinInstanceViewModel::class)
    abstract fun bindJoinInstanceViewModel(viewModel: JoinInstanceViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
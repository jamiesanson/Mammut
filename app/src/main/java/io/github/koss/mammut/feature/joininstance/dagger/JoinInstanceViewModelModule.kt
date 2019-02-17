package io.github.koss.mammut.feature.joininstance.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.ViewModelKey
import io.github.koss.mammut.feature.joininstance.JoinInstanceViewModel

@Module
abstract class JoinInstanceViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(JoinInstanceViewModel::class)
    abstract fun bindJoinInstanceViewModel(viewModel: JoinInstanceViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
package io.github.koss.mammut.toot.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.viewmodel.ViewModelKey
import io.github.koss.mammut.toot.ComposeTootViewModel

@Module
abstract class ComposeTootViewModelModule {

    @Binds
    @IntoMap
    @ComposeTootScope
    @ViewModelKey(ComposeTootViewModel::class)
    abstract fun bindComposeTootViewModel(viewModel: ComposeTootViewModel): ViewModel

    @Binds
    @ComposeTootScope
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
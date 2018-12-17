package io.github.koss.mammut.feature.instance.subfeature.feed.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.dagger.MammutViewModelFactory
import io.github.koss.mammut.dagger.ViewModelKey
import io.github.koss.mammut.feature.instance.subfeature.feed.FeedViewModel

@Module
abstract class FeedViewModelModule {

    @Binds
    @IntoMap
    @FeedScope
    @ViewModelKey(FeedViewModel::class)
    abstract fun bindFeedViewModel(viewModel: FeedViewModel): ViewModel

    @Binds
    @FeedScope
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
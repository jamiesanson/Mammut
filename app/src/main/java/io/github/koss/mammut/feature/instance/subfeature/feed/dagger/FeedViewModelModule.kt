package io.github.koss.mammut.feature.instance.subfeature.feed.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.ViewModelKey
import io.github.koss.mammut.base.dagger.scope.FeedScope
import io.github.koss.mammut.feature.instance.subfeature.feed.FeedViewModel
import io.github.koss.mammut.feature.instance.subfeature.feed.TootViewModel

@Module
abstract class FeedViewModelModule {

    @Binds
    @IntoMap
    @FeedScope
    @ViewModelKey(FeedViewModel::class)
    abstract fun bindFeedViewModel(viewModel: FeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TootViewModel::class)
    abstract fun bindTootViewModel(viewModel: TootViewModel): ViewModel

    @Binds
    @FeedScope
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
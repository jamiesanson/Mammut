package io.github.koss.mammut.feature.instance.subfeature.profile.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.ViewModelKey
import io.github.koss.mammut.feature.instance.subfeature.profile.ProfileViewModel

@Module
abstract class ProfileViewModelModule {

    @Binds
    @IntoMap
    @ProfileScope
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(viewModel: ProfileViewModel): ViewModel

    @Binds
    @ProfileScope
    abstract fun bindViewModelFactory(factory: MammutViewModelFactory): ViewModelProvider.Factory
}
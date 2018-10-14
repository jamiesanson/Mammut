package io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger

import dagger.Module
import dagger.Provides
import io.github.jamiesanson.mammut.data.models.Account

@Module(includes = [ProfileViewModelModule::class])
class ProfileModule(val account: Account?) {

    @Provides
    @ProfileScope
    fun provideAccount(): Account? = account

}
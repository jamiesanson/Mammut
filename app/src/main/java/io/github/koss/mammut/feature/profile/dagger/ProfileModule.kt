package io.github.koss.mammut.feature.profile.dagger

import dagger.Module
import dagger.Provides
import io.github.koss.mammut.base.dagger.scope.ProfileScope
import io.github.koss.mammut.data.models.Account

@Module(includes = [ProfileViewModelModule::class])
class ProfileModule(val accountId: Long?) {

    @Provides
    @ProfileScope
    fun provideAccountId(): Long? = accountId

}
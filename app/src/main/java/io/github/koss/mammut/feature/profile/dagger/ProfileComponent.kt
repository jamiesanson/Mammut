package io.github.koss.mammut.feature.profile.dagger

import dagger.Subcomponent
import io.github.koss.mammut.base.dagger.scope.ProfileScope
import io.github.koss.mammut.feature.profile.ProfileFragment

@ProfileScope
@Subcomponent(modules = [ProfileModule::class])
interface ProfileComponent {

    fun inject(fragment: ProfileFragment)
}
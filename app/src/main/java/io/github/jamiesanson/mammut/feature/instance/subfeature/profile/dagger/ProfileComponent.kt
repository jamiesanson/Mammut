package io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger

import dagger.Subcomponent
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.ProfileFragment

@ProfileScope
@Subcomponent(modules = [ProfileModule::class])
interface ProfileComponent {

    fun inject(fragment: ProfileFragment)
}
package io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger

import dagger.Subcomponent
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.ProfileController

@ProfileScope
@Subcomponent(modules = [ProfileModule::class])
interface ProfileComponent {

    fun inject(controller: ProfileController)
}
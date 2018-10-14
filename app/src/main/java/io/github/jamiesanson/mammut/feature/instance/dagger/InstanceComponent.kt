package io.github.jamiesanson.mammut.feature.instance.dagger

import dagger.Subcomponent
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedComponent
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger.ProfileComponent
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedModule
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger.ProfileModule

@InstanceScope
@Subcomponent(modules = [ InstanceModule::class ])
interface InstanceComponent {

    fun inject(instanceActivity: InstanceActivity)

    fun plus(feedModule: FeedModule): FeedComponent

    fun plus(profileModule: ProfileModule): ProfileComponent
}
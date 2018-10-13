package io.github.jamiesanson.mammut.feature.instance.dagger

import dagger.Subcomponent
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedComponent
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger.FeedModule

@InstanceScope
@Subcomponent(modules = [ InstanceModule::class ])
interface InstanceComponent {

    fun inject(instanceActivity: InstanceActivity)

    fun plus(feedModule: FeedModule): FeedComponent
}
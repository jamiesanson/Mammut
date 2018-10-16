package io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger

import dagger.Subcomponent
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedController

@FeedScope
@Subcomponent(modules = [FeedModule::class])
interface FeedComponent {

    fun inject(controller: FeedController)
}
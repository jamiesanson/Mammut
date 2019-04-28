package io.github.koss.mammut.feature.instance.subfeature.feed.dagger

import dagger.Subcomponent
import io.github.koss.mammut.base.dagger.scope.FeedScope
import io.github.koss.mammut.feature.instance.subfeature.feed.FeedController

@FeedScope
@Subcomponent(modules = [FeedModule::class])
interface FeedComponent {

    fun inject(controller: FeedController)
}
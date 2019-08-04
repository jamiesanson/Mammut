package io.github.koss.mammut.feed.dagger

import dagger.Subcomponent
import io.github.koss.mammut.feed.ui.FeedController

@FeedScope
@Subcomponent(modules = [ FeedModule::class ])
interface FeedComponent {

    fun inject(controller: FeedController)
}

package io.github.koss.mammut.feed.dagger

import dagger.Subcomponent
import io.github.koss.mammut.feed.ui.FeedController
import io.github.koss.mammut.feed.ui.FeedFragment

@FeedScope
@Subcomponent(modules = [ FeedModule::class ])
interface FeedComponent {

    fun inject(controller: FeedController)

    fun inject(fragment: FeedFragment)
}

package io.github.koss.mammut.toot.dagger

import dagger.Subcomponent
import io.github.koss.mammut.toot.ComposeTootController

@ComposeTootScope
@Subcomponent(modules = [ComposeTootModule::class])
interface ComposeTootComponent {

    fun inject(controller: ComposeTootController)
}
package io.github.koss.mammut.toot.dagger

import dagger.Subcomponent
import io.github.koss.mammut.toot.ComposeTootFragment

@ComposeTootScope
@Subcomponent(modules = [ComposeTootModule::class])
interface ComposeTootComponent {

    fun inject(fragment: ComposeTootFragment)
}
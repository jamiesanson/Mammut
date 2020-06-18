package io.github.koss.mammut.search.dagger

import dagger.Subcomponent
import io.github.koss.mammut.search.SearchFragment

@SearchScope
@Subcomponent(modules = [SearchModule::class])
interface SearchComponent {

    fun inject(fragment: SearchFragment)
}
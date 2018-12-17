package io.github.koss.mammut.feature.joininstance.dagger

import dagger.Subcomponent
import io.github.koss.mammut.feature.joininstance.JoinInstanceActivity

@JoinInstanceScope
@Subcomponent(modules = [ JoinInstanceModule::class ])
interface JoinInstanceComponent {

    fun inject(joinInstanceActivity: JoinInstanceActivity)

}
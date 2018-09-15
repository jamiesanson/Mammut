package io.github.jamiesanson.mammut.dagger.application

import dagger.Component
import io.github.jamiesanson.mammut.MammutApplication
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceComponent
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceModule

@ApplicationScope
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {

    fun inject(application: MammutApplication)

    fun plus(joinInstanceModule: JoinInstanceModule): JoinInstanceComponent
}
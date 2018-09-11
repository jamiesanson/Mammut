package io.github.jamiesanson.mammut.dagger.application

import dagger.Component
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceComponent
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceModule

@Component
interface ApplicationComponent {

    fun plus(joinInstanceModule: JoinInstanceModule): JoinInstanceComponent
}
package io.github.jamiesanson.mammut.dagger.application

import dagger.Component
import io.github.jamiesanson.mammut.MammutApplication
import io.github.jamiesanson.mammut.dagger.network.NetworkModule
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceComponent
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceModule
import io.github.jamiesanson.mammut.feature.instancebrowser.InstanceBrowserActivity
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceComponent
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceModule
import io.github.jamiesanson.mammut.feature.splash.SplashActivity

@ApplicationScope
@Component(modules = [ApplicationModule::class, NetworkModule::class])
interface ApplicationComponent {

    fun inject(application: MammutApplication)

    fun inject(activity: SplashActivity)

    fun inject(activity: InstanceBrowserActivity)

    fun plus(joinInstanceModule: JoinInstanceModule): JoinInstanceComponent

    fun plus(instanceModule: InstanceModule): InstanceComponent
}
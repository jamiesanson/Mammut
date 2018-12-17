package io.github.koss.mammut.dagger.application

import dagger.Component
import io.github.koss.mammut.MammutApplication
import io.github.koss.mammut.dagger.network.NetworkModule
import io.github.koss.mammut.feature.instance.dagger.InstanceComponent
import io.github.koss.mammut.feature.instance.dagger.InstanceModule
import io.github.koss.mammut.feature.instancebrowser.InstanceBrowserActivity
import io.github.koss.mammut.feature.joininstance.dagger.JoinInstanceComponent
import io.github.koss.mammut.feature.joininstance.dagger.JoinInstanceModule
import io.github.koss.mammut.feature.splash.SplashActivity

@ApplicationScope
@Component(modules = [ApplicationModule::class, NetworkModule::class])
interface ApplicationComponent {

    fun inject(application: MammutApplication)

    fun inject(activity: SplashActivity)

    fun inject(activity: InstanceBrowserActivity)

    fun plus(joinInstanceModule: JoinInstanceModule): JoinInstanceComponent

    fun plus(instanceModule: InstanceModule): InstanceComponent
}
package io.github.koss.mammut.feature.instance.dagger

import dagger.Subcomponent
import io.github.koss.mammut.base.dagger.scope.InstanceScope
import io.github.koss.mammut.feature.instance.InstanceActivity
import io.github.koss.mammut.feature.instance.subfeature.feed.dagger.FeedComponent
import io.github.koss.mammut.feature.instance.subfeature.profile.dagger.ProfileComponent
import io.github.koss.mammut.feature.instance.subfeature.feed.dagger.FeedModule
import io.github.koss.mammut.feature.instance.subfeature.navigation.InstanceController
import io.github.koss.mammut.feature.instance.subfeature.profile.dagger.ProfileModule
import io.github.koss.mammut.feature.settings.dagger.SettingsComponent
import io.github.koss.mammut.feature.settings.dagger.SettingsModule
import io.github.koss.mammut.toot.dagger.ComposeTootComponent
import io.github.koss.mammut.toot.dagger.ComposeTootModule
import javax.inject.Named

@InstanceScope
@Subcomponent(modules = [ InstanceModule::class ])
interface InstanceComponent {

    fun inject(instanceActivity: InstanceActivity)

    fun inject(instanceController: InstanceController)

    fun plus(feedModule: FeedModule): FeedComponent

    fun plus(profileModule: ProfileModule): ProfileComponent

    fun plus(settingsModule: SettingsModule): SettingsComponent

    fun plus(composeTootModule: ComposeTootModule): ComposeTootComponent

    @Named("instance_access_token")
    fun accessToken(): String
}
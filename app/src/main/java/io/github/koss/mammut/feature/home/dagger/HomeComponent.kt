package io.github.koss.mammut.feature.home.dagger

import dagger.Subcomponent
import io.github.koss.mammut.base.dagger.scope.HomeScope
import io.github.koss.mammut.feature.home.HomeFragment
import javax.inject.Named

@HomeScope
@Subcomponent(modules = [ HomeModule::class ])
interface HomeComponent {

    fun inject(fragment: HomeFragment)

    @Named("instance_access_token")
    fun accessToken(): String

    @Named("instance_name")
    fun instanceName(): String
}
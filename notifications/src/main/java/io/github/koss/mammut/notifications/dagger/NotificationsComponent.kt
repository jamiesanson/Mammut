package io.github.koss.mammut.notifications.dagger

import dagger.Subcomponent
import io.github.koss.mammut.notifications.NotificationsFragment


@NotificationsScope
@Subcomponent(modules = [NotificationsModule::class])
interface NotificationsComponent {

    fun inject(fragment: NotificationsFragment)
}
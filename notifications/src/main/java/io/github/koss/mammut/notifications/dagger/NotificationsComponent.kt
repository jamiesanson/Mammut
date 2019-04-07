package io.github.koss.mammut.notifications.dagger

import dagger.Subcomponent
import io.github.koss.mammut.notifications.NotificationsController


@NotificationsScope
@Subcomponent(modules = [NotificationsModule::class])
interface NotificationsComponent {

    fun inject(controller: NotificationsController)
}
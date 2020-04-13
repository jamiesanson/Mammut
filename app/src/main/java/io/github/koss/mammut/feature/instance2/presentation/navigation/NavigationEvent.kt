package io.github.koss.mammut.feature.instance2.presentation.navigation

sealed class NavigationEvent

object ScrolledUp: NavigationEvent()
object ScrolledDown: NavigationEvent()

object UserPeekRequested: NavigationEvent()
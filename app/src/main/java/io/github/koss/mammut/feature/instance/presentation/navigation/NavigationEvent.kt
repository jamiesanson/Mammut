package io.github.koss.mammut.feature.instance.presentation.navigation

sealed class NavigationEvent

object UserPeekRequested: NavigationEvent()
package io.github.koss.mammut.feature.home.presentation.navigation

sealed class NavigationEvent

object UserPeekRequested: NavigationEvent()
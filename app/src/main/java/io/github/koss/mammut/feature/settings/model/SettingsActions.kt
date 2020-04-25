package io.github.koss.mammut.feature.settings.model

/**
 * File defining the actions which can be performed on the settings screen.
 */
sealed class SettingsAction

object ToggleStreaming: SettingsAction()

object TogglePlaceKeeping: SettingsAction()

object ToggleDarkMode: SettingsAction()

object ToggleDarkModeFollowSystem: SettingsAction()
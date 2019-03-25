package io.github.koss.mammut.feature.settings.model


/**
 * File defining the actions which can be performed on the settings screen.
 */
sealed class SettingsAction

object ToggleLightDarkMode: SettingsAction()

object ToggleStreaming: SettingsAction()

object TogglePlaceKeeping: SettingsAction()

object ToggleLaunchInstanceBrowser: SettingsAction()
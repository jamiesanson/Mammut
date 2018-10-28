package io.github.jamiesanson.mammut.feature.settings.model

import androidx.annotation.StringRes

/**
 * File for defining items shown on the settings screen
 */
sealed class SettingsItem

data class SectionHeader(@StringRes val titleRes: Int, val showTopDivider: Boolean = true) : SettingsItem()

data class SettingsFooter(val appVersion: String) : SettingsItem()

data class ClickableItem(@StringRes val titleRes: Int, val action: SettingsAction) : SettingsItem()

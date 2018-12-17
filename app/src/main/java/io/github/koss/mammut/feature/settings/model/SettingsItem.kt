package io.github.koss.mammut.feature.settings.model

import androidx.annotation.StringRes

/**
 * File for defining items shown on the settings screen
 */
sealed class SettingsItem

data class SectionHeader(@StringRes val titleRes: Int) : SettingsItem()

data class SettingsFooter(val appVersion: String) : SettingsItem()

data class ClickableItem(@StringRes val titleRes: Int, val action: SettingsAction) : SettingsItem()

data class ToggleableItem(@StringRes val titleRes: Int, @StringRes val subtitleRes: Int = 0, val isSet: Boolean, val action: SettingsAction): SettingsItem()

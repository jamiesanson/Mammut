@file:Suppress("DEPRECATION")

package io.github.koss.mammut.repo

import android.content.Context
import android.preference.PreferenceManager
import io.github.koss.mammut.base.themes.Standard
import io.github.koss.mammut.base.util.boolean
import io.github.koss.mammut.base.util.string

/**
 * Utility class for managing preferences.
 */
class PreferencesRepository(appContext: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)

    var themeId by preferences.string(defaultReturn = Standard.themeName, useCommit = true)

    var loginDomain by preferences.string(defaultReturn = "")

    var isStreamingEnabled by preferences.boolean(defaultReturn = true)

    var shouldKeepFeedPlace by preferences.boolean(defaultReturn = true)

    var darkModeOverrideEnabled by preferences.boolean(defaultReturn = false)

    var darkModeFollowSystem by preferences.boolean(defaultReturn = true)

}


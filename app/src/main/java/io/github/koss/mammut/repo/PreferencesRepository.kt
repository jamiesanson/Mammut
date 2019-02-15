package io.github.koss.mammut.repo

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.github.koss.mammut.feature.themes.StandardTheme
import kotlin.reflect.KProperty

/**
 * Utility class for managing preferences.
 */
class PreferencesRepository(appContext: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)

    var themeId by preferences.string(defaultReturn = StandardTheme.themeId, useCommit = true)

    var loginDomain by preferences.string(defaultReturn = "")

    var isStreamingEnabled by preferences.boolean(defaultReturn = true)

    var shouldKeepFeedPlace by preferences.boolean(defaultReturn = true)

    var localFeedLastPageSeen by preferences.int(defaultReturn = -99999)

    var homeFeedLastPageSeen by preferences.int(defaultReturn = -99999)

    var lastAccessedInstanceToken by preferences.string("none")

    var takeMeStraightToInstanceBrowser by preferences.boolean(false)
}


internal fun SharedPreferences.string(defaultReturn: String = "", useCommit: Boolean = false) = StringDelegate(this, defaultReturn, useCommit)
internal fun SharedPreferences.int(defaultReturn: Int = 0, useCommit: Boolean = false) = IntDelegate(this, defaultReturn, useCommit)
internal fun SharedPreferences.boolean(defaultReturn: Boolean = false, useCommit: Boolean = false) = BooleanDelegate(this, defaultReturn, useCommit)

class StringDelegate(private val preferences: SharedPreferences, private val defaultReturn: String, private val useCommit: Boolean) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? =
            preferences.getString(property.name, defaultReturn)

    @SuppressLint("ApplySharedPref")
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        // If set is null, clear the preference
        if (value == null) {
            preferences.edit().remove(property.name).apply {
                if (useCommit) commit() else apply()
            }
        } else {
            preferences.edit().putString(property.name, value).apply {
                if (useCommit) commit() else apply()
            }
        }
    }
}

class IntDelegate(private val preferences: SharedPreferences, private val defaultReturn: Int, private val useCommit: Boolean) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int =
            preferences.getInt(property.name, defaultReturn)

    @SuppressLint("ApplySharedPref")
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
        // If set is null, clear the preference
        if (value == null) {
            preferences.edit().remove(property.name).apply {
                if (useCommit) commit() else apply()
            }
        } else {
            preferences.edit().putInt(property.name, value).apply {
                if (useCommit) commit() else apply()
            }
        }
    }
}

class BooleanDelegate(private val preferences: SharedPreferences, private val defaultReturn: Boolean, private val useCommit: Boolean) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            preferences.getBoolean(property.name, defaultReturn)

    @SuppressLint("ApplySharedPref")
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) {
        // If set is null, clear the preference
        if (value == null) {
            preferences.edit().remove(property.name).apply {
                if (useCommit) commit() else apply()
            }
        } else {
            preferences.edit().putBoolean(property.name, value).apply {
                if (useCommit) commit() else apply()
            }
        }
    }
}
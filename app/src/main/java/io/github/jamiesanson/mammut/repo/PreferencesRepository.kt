package io.github.jamiesanson.mammut.repo

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.github.jamiesanson.mammut.feature.themes.StandardTheme
import kotlin.reflect.KProperty

/**
 * Utility class for managing preferences.
 */
class PreferencesRepository(appContext: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)

    var themeId by preferences.string(defaultReturn = StandardTheme.themeId)

}


internal fun SharedPreferences.string(defaultReturn: String = "") = StringDelegate(this, defaultReturn)
internal fun SharedPreferences.int(defaultReturn: Int = 0) = IntDelegate(this, defaultReturn)
internal fun SharedPreferences.boolean(defaultReturn: Boolean = false) = BooleanDelegate(this, defaultReturn)

class StringDelegate(private val preferences: SharedPreferences, private val defaultReturn: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? =
            preferences.getString(property.name, defaultReturn)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        // If set is null, clear the preference
        if (value == null) {
            preferences.edit().remove(property.name).apply()
        } else {
            preferences.edit().putString(property.name, value).apply()
        }
    }
}

class IntDelegate(private val preferences: SharedPreferences, private val defaultReturn: Int) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int =
            preferences.getInt(property.name, defaultReturn)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
        // If set is null, clear the preference
        if (value == null) {
            preferences.edit().remove(property.name).apply()
        } else {
            preferences.edit().putInt(property.name, value).apply()
        }
    }
}

class BooleanDelegate(private val preferences: SharedPreferences, private val defaultReturn: Boolean) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            preferences.getBoolean(property.name, defaultReturn)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) {
        // If set is null, clear the preference
        if (value == null) {
            preferences.edit().remove(property.name).apply()
        } else {
            preferences.edit().putBoolean(property.name, value).apply()
        }
    }
}
package io.github.koss.mammut.base.util

import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlin.reflect.KProperty

fun SharedPreferences.string(defaultReturn: String = "", useCommit: Boolean = false) = StringDelegate(this, defaultReturn, useCommit)
fun SharedPreferences.int(defaultReturn: Int = 0, useCommit: Boolean = false) = IntDelegate(this, defaultReturn, useCommit)
fun SharedPreferences.boolean(defaultReturn: Boolean = false, useCommit: Boolean = false) = BooleanDelegate(this, defaultReturn, useCommit)

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
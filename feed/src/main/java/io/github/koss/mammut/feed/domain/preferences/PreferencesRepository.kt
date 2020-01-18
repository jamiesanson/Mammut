package io.github.koss.mammut.feed.domain.preferences

import android.content.Context
import android.preference.PreferenceManager
import io.github.koss.mammut.base.util.int

class PreferencesRepository(appContext: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)

    var localFeedLastPositionSeen by preferences.int(defaultReturn = -99999)

    var homeFeedLastPositionSeen by preferences.int(defaultReturn = -99999)

}
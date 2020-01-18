package io.github.koss.mammut.feature.settings

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.koss.mammut.BuildConfig
import io.github.koss.mammut.R
import io.github.koss.mammut.base.themes.Theme
import io.github.koss.mammut.base.util.postSafely
import io.github.koss.mammut.repo.PreferencesRepository
import io.github.koss.mammut.feature.base.Event
import io.github.koss.mammut.feature.settings.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
        private val preferencesRepository: PreferencesRepository
) : ViewModel(), CoroutineScope by GlobalScope {

    val settingsItems: LiveData<List<SettingsItem>> = MutableLiveData()

    val restartApp: LiveData<Event<Unit>> = MutableLiveData()

    init {
        rebuildSettingsScreen()
    }

    private fun rebuildSettingsScreen() {
        settingsItems.postSafely(
                listOfNotNull(
                        ToggleableItem(
                                titleRes = R.string.dark_mode,
                                isSet = preferencesRepository.darkModeOverrideEnabled,
                                action = ToggleDarkMode,
                                isEnabled = if (Build.VERSION.SDK_INT >= 29) !preferencesRepository.darkModeFollowSystem else true
                        ),
                        ToggleableItem(
                                titleRes = R.string.dark_mode_follow_system,
                                subtitleRes = R.string.dark_mode_follow_system_description,
                                isSet = preferencesRepository.darkModeFollowSystem,
                                action = ToggleDarkModeFollowSystem
                        ).takeIf { Build.VERSION.SDK_INT >= 29 },
                        SectionHeader(
                                titleRes = R.string.app_settings
                        ),
                        ToggleableItem(
                                titleRes = R.string.disable_streaming,
                                subtitleRes = R.string.disable_streaming_subtitle,
                                isSet = !preferencesRepository.isStreamingEnabled,
                                action = ToggleStreaming
                        ),
                        ToggleableItem(
                                titleRes = R.string.keep_your_place,
                                subtitleRes = R.string.well_keep_your_place,
                                isSet = preferencesRepository.shouldKeepFeedPlace,
                                action = TogglePlaceKeeping
                        ),
                        ToggleableItem(
                                titleRes = R.string.swipe_between_instances,
                                isSet = preferencesRepository.swipingBetweenInstancesEnabled,
                                action = ToggleSwipingBetweenInstance
                        )
                )
        )
    }

    fun performAction(settingsAction: SettingsAction) {
        when (settingsAction) {
            ToggleStreaming -> {
                preferencesRepository.isStreamingEnabled = !preferencesRepository.isStreamingEnabled
                rebuildSettingsScreen()
            }
            TogglePlaceKeeping -> {
                preferencesRepository.shouldKeepFeedPlace = !preferencesRepository.shouldKeepFeedPlace
                rebuildSettingsScreen()
            }
            ToggleSwipingBetweenInstance -> {
                preferencesRepository.swipingBetweenInstancesEnabled = !preferencesRepository.swipingBetweenInstancesEnabled
                rebuildSettingsScreen()
            }
            ToggleDarkMode -> {
                preferencesRepository.darkModeOverrideEnabled = !preferencesRepository.darkModeOverrideEnabled
                restartApp.postSafely(Event(Unit))
                rebuildSettingsScreen()
            }
            ToggleDarkModeFollowSystem -> {
                preferencesRepository.darkModeFollowSystem = !preferencesRepository.darkModeFollowSystem
                restartApp.postSafely(Event(Unit))
                rebuildSettingsScreen()
            }
        }
    }

    fun onThemeChanged(theme: Theme) {
        if (theme.themeName == preferencesRepository.themeId) return

        preferencesRepository.themeId = theme.themeName

        restartApp.postSafely(Event(Unit))
        rebuildSettingsScreen()
    }

}
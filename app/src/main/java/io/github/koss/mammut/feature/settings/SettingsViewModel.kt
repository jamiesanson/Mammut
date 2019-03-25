package io.github.koss.mammut.feature.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.koss.mammut.R
import io.github.koss.mammut.base.themes.StandardLightTheme
import io.github.koss.mammut.base.themes.StandardTheme
import io.github.koss.mammut.repo.PreferencesRepository
import io.github.koss.mammut.extension.postSafely
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
                listOf(
                        SectionHeader(
                                titleRes = R.string.app_settings
                        ),
                        ToggleableItem(
                                titleRes = R.string.enable_light_mode,
                                isSet = preferencesRepository.themeId == StandardLightTheme.themeId,
                                action = ToggleLightDarkMode
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
                                titleRes = R.string.launch_chooser_title,
                                subtitleRes = R.string.launch_chooser_description,
                                isSet = preferencesRepository.takeMeStraightToInstanceBrowser,
                                action = ToggleLaunchInstanceBrowser
                        )
                )
        )
    }

    fun performAction(settingsAction: SettingsAction) {
        when (settingsAction) {
            ToggleLightDarkMode -> {
                preferencesRepository.themeId = when (preferencesRepository.themeId) {
                    StandardTheme.themeId -> StandardLightTheme.themeId
                    StandardLightTheme.themeId -> StandardTheme.themeId
                    else -> StandardTheme.themeId
                }

                restartApp.postSafely(Event(Unit))
                rebuildSettingsScreen()
            }
            ToggleStreaming -> {
                preferencesRepository.isStreamingEnabled = !preferencesRepository.isStreamingEnabled
                rebuildSettingsScreen()
            }
            TogglePlaceKeeping -> {
                preferencesRepository.shouldKeepFeedPlace = !preferencesRepository.shouldKeepFeedPlace
                rebuildSettingsScreen()
            }
            ToggleLaunchInstanceBrowser -> {
                preferencesRepository.takeMeStraightToInstanceBrowser = !preferencesRepository.takeMeStraightToInstanceBrowser
                rebuildSettingsScreen()
            }
        }
    }

}
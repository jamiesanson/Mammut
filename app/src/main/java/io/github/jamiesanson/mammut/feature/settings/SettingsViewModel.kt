package io.github.jamiesanson.mammut.feature.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.jamiesanson.mammut.BuildConfig
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.data.repo.PreferencesRepository
import io.github.jamiesanson.mammut.data.repo.RegistrationRepository
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.feature.base.Event
import io.github.jamiesanson.mammut.feature.instance.dagger.InstanceScope
import io.github.jamiesanson.mammut.feature.settings.model.*
import io.github.jamiesanson.mammut.feature.themes.StandardLightTheme
import io.github.jamiesanson.mammut.feature.themes.StandardTheme
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject
import javax.inject.Named

class SettingsViewModel @Inject constructor(
        private val preferencesRepository: PreferencesRepository,
        private val registrationRepository: RegistrationRepository,
        @InstanceScope
        @Named("instance_access_token")
        private val accessToken: String
) : ViewModel() {

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
                        SectionHeader(
                                titleRes = R.string.account_settings
                        ),
                        ClickableItem(
                                titleRes = R.string.change_instance,
                                action = ChangeInstance
                        ),
                        ClickableItem(
                                titleRes = R.string.log_out,
                                action = LogOut
                        ),
                        SectionHeader(
                                titleRes = R.string.about_mammut
                        ),
                        ClickableItem(
                                titleRes = R.string.contributors,
                                action = NavigationAction.ViewContributors
                        ),
                        ClickableItem(
                                titleRes = R.string.open_source_licenses,
                                action = ViewOssLicenses
                        ),
                        SettingsFooter(
                                appVersion = "${BuildConfig.VERSION_NAME}/${BuildConfig.BUILD_TYPE}"
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
            LogOut -> {
                launch {
                    val registration = registrationRepository.getAllRegistrations()
                            .first { it.accessToken?.accessToken == accessToken }

                    registrationRepository.logOut(registration.id)
                }
            }
        }
    }

}
package io.github.jamiesanson.mammut.feature.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.jamiesanson.mammut.BuildConfig
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.feature.settings.model.*
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
): ViewModel() {

    val settingsItems: LiveData<List<SettingsItem>> = MutableLiveData()

    init {
        // TODO - This should probably changed based off some advanced member preference
        settingsItems.postSafely(
                listOf(
                        SectionHeader(titleRes = R.string.app_settings, showTopDivider = false),
                        SectionHeader(titleRes = R.string.account_settings),
                        ClickableItem(titleRes = R.string.log_out, action = LogOut),
                        SectionHeader(titleRes = R.string.about_mammut),
                        ClickableItem(titleRes = R.string.contributors, action = NavigationAction.ViewContributors),
                        SettingsFooter(appVersion = "${BuildConfig.VERSION_NAME}/${BuildConfig.BUILD_TYPE}")
                )
        )
    }

    fun performAction(settingsAction: SettingsAction) {

    }

}
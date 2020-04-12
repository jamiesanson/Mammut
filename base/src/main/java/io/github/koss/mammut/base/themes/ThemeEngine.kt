package io.github.koss.mammut.base.themes

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.CollapsingToolbarLayout

class ThemeEngine(
        private val config: ThemeConfig
) {

    val allThemes = setOf(
            Standard,
            PastelGreen
    )

    val currentTheme: Theme
        get() = allThemes.find { it.themeName == config.currentThemeId } ?: run {
                config.currentThemeId = Standard.themeName
                Standard
            }

    fun apply(activity: AppCompatActivity) {
        if (!config.darkModeFollowSystem) {
            activity.delegate.localNightMode = if (config.darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        } else {
            activity.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        activity.setTheme(currentTheme.styleRes)
    }

    fun applyFontToCollapsingLayout(collapsingToolbarLayout: CollapsingToolbarLayout) {
        val typeface = Typeface.createFromAsset(collapsingToolbarLayout.context.assets, currentTheme.primaryFont.path)
        with(collapsingToolbarLayout) {
            setCollapsedTitleTypeface(typeface)
            setExpandedTitleTypeface(typeface)
        }
    }
}
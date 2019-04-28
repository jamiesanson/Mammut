package io.github.koss.mammut.base.themes

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.CollapsingToolbarLayout
import io.github.koss.mammut.base.R
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class ThemeEngine(
        private val config: ThemeConfig
) {

    val allThemes = setOf(
            Standard,
            StandardLight,
            PastelGreen,
            PastelGreenLight
    )

    val currentTheme: Theme
        get() = allThemes.find { it.themeName == config.currentThemeId } ?: run {
                config.currentThemeId = Standard.themeName
                Standard
            }

    val isLightTheme: Boolean
        get() = currentTheme.lightTheme

    fun apply(activity: AppCompatActivity) {
        activity.setTheme(currentTheme.styleRes)
        updateFontDefaults()
    }

    fun updateFontDefaults() {
        CalligraphyConfig.initDefault(CalligraphyConfig.Builder()
                .setDefaultFontPath(currentTheme.primaryFont.path)
                .setFontAttrId(R.attr.fontPath)
                .build()
        )
    }

    fun applyFontToCollapsingLayout(collapsingToolbarLayout: CollapsingToolbarLayout) {
        val typeface = Typeface.createFromAsset(collapsingToolbarLayout.context.assets, currentTheme.primaryFont.path)
        with(collapsingToolbarLayout) {
            setCollapsedTitleTypeface(typeface)
            setExpandedTitleTypeface(typeface)
        }
    }
}
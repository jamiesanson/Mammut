package io.github.jamiesanson.mammut.feature.themes

import android.graphics.Typeface
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.CollapsingToolbarLayout
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.data.repo.PreferencesRepository
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class ThemeEngine(
        private val preferencesRepository: PreferencesRepository
) {

    private val currentTheme: Theme
       get() = when (preferencesRepository.themeId) {
           StandardTheme.themeId -> StandardTheme
           StandardLightTheme.themeId -> StandardLightTheme
           else -> {
               preferencesRepository.themeId = StandardTheme.themeId
               Log.w("ThemeEngine", "Invalid theme ID. Resetting to standard")
               StandardTheme
           }
       }

    val isLightTheme: Boolean
        get() = currentTheme.themeId.contains("light")

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
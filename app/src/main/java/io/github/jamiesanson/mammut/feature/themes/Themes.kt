package io.github.jamiesanson.mammut.feature.themes

import androidx.annotation.StyleRes
import io.github.jamiesanson.mammut.R

sealed class Theme(
        val themeId: String,
        @StyleRes val styleRes: Int,
        val primaryFont: Font,
        val secondaryFont: Font
)

/**
 * The standard theme for Mammut
 */
object StandardTheme: Theme(
        themeId = "standard",
        styleRes = R.style.Mammut_Standard,
        primaryFont = VarelaRound,
        secondaryFont = Poppins)
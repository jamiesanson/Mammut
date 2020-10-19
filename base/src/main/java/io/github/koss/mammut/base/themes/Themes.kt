package io.github.koss.mammut.base.themes

import androidx.annotation.StyleRes
import io.github.koss.mammut.base.R

sealed class Theme(
        val themeName: String,
        @StyleRes val styleRes: Int,
        val primaryFont: Font
)

/**
 * The standard theme for Mammut
 */
object Standard: Theme(
        themeName = "Standard",
        styleRes = R.style.Theme_Mammut_Standard,
        primaryFont = VarelaRound)

/**
 * Standard light theme for Mammut
 */
object PastelGreen: Theme(
        themeName = "Pastel Green",
        styleRes = R.style.Theme_Mammut_Pastel_Green,
        primaryFont = VarelaRound
)
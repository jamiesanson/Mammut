package io.github.koss.mammut.base.themes

import androidx.annotation.StyleRes
import io.github.koss.mammut.base.R

sealed class Theme(
        val themeName: String,
        @StyleRes val styleRes: Int,
        val primaryFont: Font,
        val secondaryFont: Font,
        val lightTheme: Boolean = false
)

/**
 * The standard theme for Mammut
 */
object Standard: Theme(
        themeName = "Standard",
        styleRes = R.style.Mammut_Standard,
        primaryFont = VarelaRound,
        secondaryFont = Poppins)

/**
 * Standard light theme for Mammut
 */
object StandardLight: Theme(
        themeName = "Standard - Light",
        styleRes = R.style.Mammut_Standard_Light,
        primaryFont = VarelaRound,
        secondaryFont = Poppins,
        lightTheme = true
)

/**
 * Standard light theme for Mammut
 */
object PastelGreen: Theme(
        themeName = "Pastel Green",
        styleRes = R.style.Mammut_Pastel_Green,
        primaryFont = VarelaRound,
        secondaryFont = Poppins
)

/**
 * Standard light theme for Mammut
 */
object PastelGreenLight: Theme(
        themeName = "Pastel Green - Light",
        styleRes = R.style.Mammut_Pastel_Green_Light,
        primaryFont = VarelaRound,
        secondaryFont = Poppins,
        lightTheme = true
)
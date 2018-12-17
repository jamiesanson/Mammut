package io.github.koss.mammut.feature.themes

sealed class Font(val path: String)

object VarelaRound: Font("fonts/VarelaRound-Regular.ttf")

object Poppins: Font("fonts/Poppins-Regular.ttf")
package io.github.koss.mammut.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Emoji(
        val shortcode: String = "",
        val staticUrl: String = "",
        val url: String = "",
        val visibleInPicker: Boolean = true
): Parcelable

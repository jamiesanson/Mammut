package io.github.jamiesanson.mammut.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Emoji(
        val shortcode: String = "",
        val staticUrl: String = "",
        val url: String = "",
        val visibleInPicker: Boolean = true
): Parcelable

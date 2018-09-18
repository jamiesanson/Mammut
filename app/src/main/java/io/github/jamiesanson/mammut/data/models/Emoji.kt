package io.github.jamiesanson.mammut.data.models

data class Emoji(
        val shortcode: String = "",
        val staticUrl: String = "",
        val url: String = "",
        val visibleInPicker: Boolean = true)

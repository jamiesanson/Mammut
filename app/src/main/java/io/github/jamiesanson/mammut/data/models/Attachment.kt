package io.github.jamiesanson.mammut.data.models


data class Attachment(
        val attachmentId: Long = 0L,
        val attachmentType: String = Type.Image.value,
        val attachmentUrl: String = "",
        val remoteUrl: String? = null,
        val previewUrl: String = "",
        val textUrl: String? = null) {
    enum class Type(val value: String) {
        Image("image"),
        Video("video"),
        Gifv("gifv")
    }
}

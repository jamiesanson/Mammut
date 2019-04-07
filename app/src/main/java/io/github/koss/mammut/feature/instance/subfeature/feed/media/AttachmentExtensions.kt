package io.github.koss.mammut.feature.instance.subfeature.feed.media

import com.sys1yagi.mastodon4j.api.entity.Attachment
import com.sys1yagi.mastodon4j.api.entity.GifvAttachment
import com.sys1yagi.mastodon4j.api.entity.PhotoAttachment
import com.sys1yagi.mastodon4j.api.entity.VideoAttachment

/**
 * Function for inspecting an attachment for metadata and retrieving an approximate
 * width and height. Will assume 4:3 width:height ratio if nothing found
 */
fun Attachment<*>.getThumbnailSpec(): Float {
    val bestGuess = 400F / 300F

    @Suppress("USELESS_ELVIS")
    this ?: return bestGuess

    return when (this) {
        is PhotoAttachment -> this.metadata?.original?.run {
            when {
                aspect != 0F -> aspect
                width != 0 && height != 0 -> width.toFloat() / height.toFloat()
                else -> bestGuess
            }
        } ?: bestGuess
        is VideoAttachment -> this.metadata?.original?.run {
            if (width != 0 && height != 0) width.toFloat() / height.toFloat() else bestGuess
        } ?: bestGuess
        is GifvAttachment -> this.metadata?.original?.run {
            if (width != 0 && height != 0) width.toFloat() / height.toFloat() else bestGuess
        } ?: bestGuess
        else -> throw IllegalArgumentException("Unknown attachment type")
    }
}
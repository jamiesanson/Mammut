package io.github.koss.emoji

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestOptions.overrideOf
import com.sys1yagi.mastodon4j.api.entity.Emoji
import kotlinx.coroutines.coroutineScope
import java.lang.Exception

/**
 * Emoji rendering class, used to encapsulate logic around loading and displaying emoji
 */
object EmojiRenderer {

    /**
     * Main render function, takes context, list of emojis and line height, and builds a [Spannable]
     * based on the input [status]
     */
    suspend fun render(context: Context, status: CharSequence, emojis: List<Emoji>, lineHeight: Int = 64): SpannableStringBuilder = coroutineScope {
        // Iterate through the status, picking out emojis and their start index
        val foundEmojis = status.mapIndexed { index, char -> if (char == ':') index else null }
            .asSequence()
            .filterNotNull()
            .windowed(2, 1)
            .map { (start, end) -> (start to end) to status.substring(start + 1 until end) }
            .map { (indices, text) -> indices to emojis.find { it.shortcode == text }}
            .filter { (_, emoji) -> emoji != null }
            .toList()

        // Build spannable
        return@coroutineScope SpannableStringBuilder().apply {
            append(status)
            foundEmojis.sortedBy { it.first.first }.forEach { (indices, emoji) ->
                try {
                    // Load image synchronously
                    val emojiDrawable = Glide.with(context)
                        .asBitmap()
                        .load(emoji!!.url)
                        .apply(overrideOf(lineHeight))
                        .submit()
                        .get()

                    // Apply image span in place of emoji shortcode
                    setSpan(ImageSpan(context, emojiDrawable), indices.first, indices.second + 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                } catch (exception: Exception) {
                    // If an exception occurs, who cares. Log the exception and keep going.
                    (exception as? GlideException)?.logRootCauses("EmojiRenderer")
                }
            }
        }
    }
}
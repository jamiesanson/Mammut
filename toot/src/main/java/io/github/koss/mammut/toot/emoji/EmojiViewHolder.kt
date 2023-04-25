package io.github.koss.mammut.toot.emoji

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.circleCropTransform
import com.sys1yagi.mastodon4j.api.entity.Emoji
import io.github.koss.mammut.base.anko.colorAttr
import io.github.koss.mammut.base.util.GlideApp
import io.github.koss.mammut.toot.R
import io.github.koss.mammut.toot.databinding.ViewHolderEmojiBinding

class EmojiViewHolder(
        itemView: View
) : RecyclerView.ViewHolder(itemView) {

    private val binding = ViewHolderEmojiBinding.bind(itemView)

    fun bind(emoji: Emoji, clickCallback: (Emoji) -> Unit) {
        val color = itemView.context.colorAttr(R.attr.colorOnSurface)

        GlideApp.with(itemView)
                .load(emoji.url)
                .placeholder(ColorDrawable(color).apply {
                    alpha = ((255f / 100f) * 54f).toInt()
                })
                .thumbnail(
                        Glide.with(itemView)
                                .load(ColorDrawable(color).apply { alpha = ((255f / 100f) * 54f).toInt() })
                                .apply(circleCropTransform())
                )
                .into(binding.emojiImageView)

        binding.emojiImageView.setOnClickListener {
            clickCallback(emoji)
        }

        TooltipCompat.setTooltipText(binding.emojiImageView, emoji.shortcode)
    }

}
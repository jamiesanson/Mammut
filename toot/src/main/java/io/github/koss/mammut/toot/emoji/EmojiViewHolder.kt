package io.github.koss.mammut.toot.emoji

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sys1yagi.mastodon4j.api.entity.Emoji
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_emoji.*

class EmojiViewHolder(
        itemView: View
): RecyclerView.ViewHolder(itemView), LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(emoji: Emoji, clickCallback: (Emoji) -> Unit) {
        Glide.with(itemView)
                .load(emoji.url)
                .into(emojiImageView)

        emojiImageView.setOnClickListener {
            clickCallback(emoji)
        }

        emojiImageView.contentDescription = emoji.shortcode
    }

}
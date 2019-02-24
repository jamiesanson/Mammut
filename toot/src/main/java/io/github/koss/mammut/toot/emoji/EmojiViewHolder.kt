package io.github.koss.mammut.toot.emoji

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions.circleCropTransform
import com.sys1yagi.mastodon4j.api.entity.Emoji
import io.github.koss.mammut.toot.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_emoji.*
import org.jetbrains.anko.colorAttr

class EmojiViewHolder(
        itemView: View
) : RecyclerView.ViewHolder(itemView), LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(emoji: Emoji, clickCallback: (Emoji) -> Unit) {
        val color = itemView.context.colorAttr(R.attr.colorPrimaryTransparency)

        Glide.with(itemView)
                .load(emoji.url)
                .thumbnail(
                        Glide.with(itemView)
                                .load(ColorDrawable(color))
                                .apply(circleCropTransform())
                )
                .into(emojiImageView)

        emojiImageView.setOnClickListener {
            clickCallback(emoji)
        }

        TooltipCompat.setTooltipText(emojiImageView, emoji.shortcode)
    }

}
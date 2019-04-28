package io.github.koss.mammut.toot.emoji

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sys1yagi.mastodon4j.api.entity.Emoji
import io.github.koss.mammut.toot.R

class EmojiAdapter(
        private val onEmojiClicked: (Emoji) -> Unit
) : ListAdapter<Emoji, EmojiViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder =
            EmojiViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.view_holder_emoji, parent, false)
            )

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(getItem(position), onEmojiClicked)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Emoji>() {
            override fun areItemsTheSame(oldItem: Emoji, newItem: Emoji): Boolean =
                    oldItem.shortcode == newItem.shortcode

            override fun areContentsTheSame(oldItem: Emoji, newItem: Emoji): Boolean =
                    oldItem == newItem
        }
    }
}
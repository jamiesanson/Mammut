package io.github.koss.mammut.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.koss.mammut.data.models.Emoji

@Entity
data class EmojiListEntity(
        @PrimaryKey
        val instance: String,
        val emojis: ArrayList<Emoji>
)
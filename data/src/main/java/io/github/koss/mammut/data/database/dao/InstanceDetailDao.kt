package io.github.koss.mammut.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import io.github.koss.mammut.data.database.entities.EmojiListEntity

@Dao
interface InstanceDetailDao {

    @Query("SELECT * FROM emojilistentity WHERE instance = :url LIMIT 1")
    fun getEmojisForUrl(url: String): EmojiListEntity
}
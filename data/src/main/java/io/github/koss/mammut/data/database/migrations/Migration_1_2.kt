package io.github.koss.mammut.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.intellij.lang.annotations.Language

@Suppress("ClassName")
object Migration_1_2: Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // Create EmojiList table
        @Language("RoomSql")
        val query = "CREATE TABLE IF NOT EXISTS `emojilistentity` (`instance` TEXT NOT NULL, `emojis` TEXT NOT NULL, `retrievalTimeMs` INTEGER NOT NULL, PRIMARY KEY(`instance`))"

        database.execSQL(query)
    }
}
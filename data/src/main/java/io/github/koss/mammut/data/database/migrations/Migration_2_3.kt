package io.github.koss.mammut.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.intellij.lang.annotations.Language

@Suppress("ClassName")
object Migration_2_3: Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // Create EmojiList table
        @Language("RoomSql")
        val query = "ALTER TABLE instanceregistrationentity ADD COLUMN orderIndex INTEGER DEFAULT -1 NOT NULL;"

        database.execSQL(query)
    }
}
package io.github.koss.mammut.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.koss.mammut.data.converters.RoomConverters
import io.github.koss.mammut.data.database.dao.StatusDao
import io.github.koss.mammut.data.database.entities.feed.Status

@Database(
        entities = [
            Status::class
        ],
        version = 1,
        exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class StatusDatabase: RoomDatabase() {

    abstract fun statusDao(): StatusDao
}
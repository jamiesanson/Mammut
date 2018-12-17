package io.github.koss.mammut.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.koss.mammut.data.converters.RoomConverters
import io.github.koss.mammut.data.database.dao.InstanceRegistrationDao
import io.github.koss.mammut.data.database.dao.InstanceSearchDao
import io.github.koss.mammut.data.database.entities.InstanceRegistrationEntity
import io.github.koss.mammut.data.database.entities.InstanceSearchResultEntity

@Database(
        entities = [
            InstanceRegistrationEntity::class,
            InstanceSearchResultEntity::class
        ],
        version = 1)
@TypeConverters(RoomConverters::class)
abstract class MammutDatabase: RoomDatabase() {

    abstract fun instanceRegistrationDao(): InstanceRegistrationDao

    abstract fun instanceSearchDao(): InstanceSearchDao
}
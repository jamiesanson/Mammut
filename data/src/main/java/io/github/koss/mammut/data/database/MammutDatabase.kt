package io.github.koss.mammut.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.koss.mammut.data.converters.RoomConverters
import io.github.koss.mammut.data.database.dao.InstanceDetailDao
import io.github.koss.mammut.data.database.dao.InstanceRegistrationDao
import io.github.koss.mammut.data.database.dao.InstanceSearchDao
import io.github.koss.mammut.data.database.entities.EmojiListEntity
import io.github.koss.mammut.data.database.entities.InstanceRegistrationEntity
import io.github.koss.mammut.data.database.entities.InstanceSearchResultEntity

@Database(
        entities = [
            InstanceRegistrationEntity::class,
            InstanceSearchResultEntity::class,
            EmojiListEntity::class
        ],
        version = 3)
@TypeConverters(RoomConverters::class)
abstract class MammutDatabase: RoomDatabase() {


    abstract fun instanceRegistrationDao(): InstanceRegistrationDao

    abstract fun instanceSearchDao(): InstanceSearchDao

    abstract fun instanceDetailDao(): InstanceDetailDao
}
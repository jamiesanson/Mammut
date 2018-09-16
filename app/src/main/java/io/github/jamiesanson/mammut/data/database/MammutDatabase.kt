package io.github.jamiesanson.mammut.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.jamiesanson.mammut.data.database.dao.InstanceRegistrationDao
import io.github.jamiesanson.mammut.data.database.entities.InstanceRegistrationEntity


@Database(
        entities = [
            InstanceRegistrationEntity::class
        ],
        version = 1)
abstract class MammutDatabase: RoomDatabase() {

    abstract fun instanceRegistrationDao(): InstanceRegistrationDao
}
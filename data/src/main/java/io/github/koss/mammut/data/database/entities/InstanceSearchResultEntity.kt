package io.github.koss.mammut.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InstanceSearchResultEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        val name: String,
        val users: Long
)
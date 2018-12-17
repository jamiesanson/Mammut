package io.github.koss.mammut.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.koss.mammut.data.database.entities.InstanceSearchResultEntity

@Dao
interface InstanceSearchDao {

    @Insert
    fun addAllResults(results: List<InstanceSearchResultEntity>)

    @Query("SELECT * FROM instancesearchresultentity")
    fun getAllResults(): List<InstanceSearchResultEntity>
}
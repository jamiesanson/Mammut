package io.github.jamiesanson.mammut.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.jamiesanson.mammut.data.database.entities.InstanceRegistrationEntity

@Dao
interface InstanceRegistrationDao {

    @Query("SELECT * FROM instanceregistrationentity")
    fun getAllRegistrations(): List<InstanceRegistrationEntity>

    @Query("SELECT * FROM instanceregistrationentity WHERE instanceName = :name")
    fun getRegistrationByName(name: String): InstanceRegistrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRegistration(registration: InstanceRegistrationEntity): Long

}
package io.github.jamiesanson.mammut.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.github.jamiesanson.mammut.data.database.entities.InstanceRegistrationEntity

@Dao
interface InstanceRegistrationDao {

    @Query("SELECT * FROM instanceregistrationentity")
    fun getAllRegistrations(): List<InstanceRegistrationEntity>

    @Query("SELECT * FROM instanceregistrationentity")
    fun getAllRegistrationsLive(): LiveData<List<InstanceRegistrationEntity>>

    @Query("SELECT * FROM instanceregistrationentity WHERE instanceName = :name LIMIT 1")
    fun getRegistrationByName(name: String): InstanceRegistrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRegistration(registration: InstanceRegistrationEntity): Long

    @Query("DELETE FROM instanceregistrationentity WHERE id = :id")
    fun deleteRegistration(id: Long)

}
package io.github.koss.mammut.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.github.koss.mammut.data.database.entities.InstanceRegistrationEntity

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

    @Transaction
    fun swapOrderedItems(fromIndex: Int, toIndex: Int) {
        var regos = getAllRegistrations().sortedBy { it.orderIndex }.filter { it.accessToken != null }

        // Check for no ordering
        if (regos.all { it.orderIndex == -1}) {
            regos.forEachIndexed { index, instanceRegistrationEntity ->
                insertRegistration(instanceRegistrationEntity.copy(orderIndex = index))
            }

            regos = getAllRegistrations().filter { it.accessToken != null }
        }

        val movingRego = regos[fromIndex]
        val otherRepo = regos[toIndex]

        insertRegistration(movingRego.copy(orderIndex = otherRepo.orderIndex))
        insertRegistration(otherRepo.copy(orderIndex = movingRego.orderIndex))
    }

}
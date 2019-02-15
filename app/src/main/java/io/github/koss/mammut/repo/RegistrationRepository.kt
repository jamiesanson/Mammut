package io.github.koss.mammut.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.data.converters.toEntity
import io.github.koss.mammut.data.converters.toModel
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.extension.filterElements
import javax.inject.Inject

/**
 * Repository masking data functions relating to registration
 */
@ApplicationScope
class RegistrationRepository @Inject constructor(
        private val database: MammutDatabase
) {

    suspend fun addOrUpdateRegistration(registration: InstanceRegistration) {
        database.instanceRegistrationDao().insertRegistration(registration = registration.toEntity())
    }

    suspend fun getRegistrationForName(name: String): InstanceRegistration? = database.instanceRegistrationDao().getRegistrationByName(name)?.toModel()

    suspend fun hasRegistrations(): Boolean = database.instanceRegistrationDao().getAllRegistrations().isNotEmpty()

    suspend fun getAllRegistrations(): List<InstanceRegistration> = database.instanceRegistrationDao().getAllRegistrations().map { it.toModel() }

    fun getAllRegistrationsLive(): LiveData<List<InstanceRegistration>> = Transformations.map(database.instanceRegistrationDao().getAllRegistrationsLive()) { it -> it.map { it.toModel() } }

    fun getAllCompletedRegistrationsLive(): LiveData<List<InstanceRegistration>> = getAllRegistrationsLive()
            .filterElements {
                it.account != null
            }

    suspend fun logOut(id: Long) = database.instanceRegistrationDao().deleteRegistration(id)
}
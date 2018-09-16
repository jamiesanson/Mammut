package io.github.jamiesanson.mammut.data.repo

import io.github.jamiesanson.mammut.dagger.application.ApplicationScope
import io.github.jamiesanson.mammut.data.converters.toEntity
import io.github.jamiesanson.mammut.data.converters.toModel
import io.github.jamiesanson.mammut.data.database.MammutDatabase
import io.github.jamiesanson.mammut.data.models.InstanceRegistration
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

    suspend fun getRegistrationForName(name: String): InstanceRegistration?
        = database.instanceRegistrationDao().getRegistrationByName(name)?.toModel()

    suspend fun hasRegistrations(): Boolean
        = database.instanceRegistrationDao().getAllRegistrations().isNotEmpty()

    suspend fun getAllRegistrations(): List<InstanceRegistration>
        = database.instanceRegistrationDao().getAllRegistrations().map { it.toModel() }
}
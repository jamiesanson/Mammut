package io.github.koss.mammut.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import io.github.koss.mammut.base.util.filterElements
import io.github.koss.mammut.dagger.application.ApplicationScope
import io.github.koss.mammut.data.converters.toLocalModel
import io.github.koss.mammut.data.converters.toNetworkModel
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.models.InstanceRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Repository masking data functions relating to registration
 */
@ApplicationScope
class RegistrationRepository @Inject constructor(
        private val database: MammutDatabase
) {

    fun moveRegistrationOrdering(fromIndex: Int, toIndex: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            database.instanceRegistrationDao().swapOrderedItems(fromIndex, toIndex)
        }
    }

    suspend fun addOrUpdateRegistration(registration: InstanceRegistration) {
        database.instanceRegistrationDao().insertRegistration(registration = registration.toLocalModel())
    }

    suspend fun getRegistrationForName(name: String): InstanceRegistration? = database.instanceRegistrationDao().getRegistrationByName(name)?.toNetworkModel()

    suspend fun hasRegistrations(): Boolean = database.instanceRegistrationDao().getAllRegistrations().isNotEmpty()

    suspend fun getAllRegistrations(): List<InstanceRegistration> = database.instanceRegistrationDao().getAllRegistrations().map { it.toNetworkModel() }

    fun getAllRegistrationsLive(): LiveData<List<InstanceRegistration>> = Transformations.map(database.instanceRegistrationDao().getAllRegistrationsLive()) { it -> it.map { it.toNetworkModel() } }

    fun getAllCompletedRegistrationsLive(): LiveData<List<InstanceRegistration>> = Transformations.map(getAllRegistrationsLive()
            .filterElements {
                it.account != null
            }) { it.sortedBy { item -> item.orderIndex }}

    suspend fun logOut(id: Long) = database.instanceRegistrationDao().deleteRegistration(id)
}
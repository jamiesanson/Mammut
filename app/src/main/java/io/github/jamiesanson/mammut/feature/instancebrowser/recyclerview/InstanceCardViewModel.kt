package io.github.jamiesanson.mammut.feature.instancebrowser.recyclerview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.jamiesanson.mammut.data.models.InstanceRegistration
import io.github.jamiesanson.mammut.data.remote.response.InstanceDetail
import io.github.jamiesanson.mammut.data.repo.InstancesRepository
import io.github.jamiesanson.mammut.extension.postSafely
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

class InstanceCardViewModel @Inject constructor(
    private val instancesRepository: InstancesRepository
) : ViewModel() {

    val title: LiveData<String> = MutableLiveData()

    val instanceInformation: LiveData<InstanceDetail?> = MutableLiveData()

    val registrationInformation: LiveData<InstanceRegistration> = MutableLiveData()

    fun initialise(instanceRegistration: InstanceRegistration) {
        // Only reload information if the current instance isn't correct
        if (registrationInformation.value != instanceRegistration) {
            registrationInformation.postSafely(instanceRegistration)
            title.postSafely("@${instanceRegistration.account?.userName}@${instanceRegistration.instanceName}")

            launch {
                val info = instancesRepository.getInstanceInformation(instanceRegistration.instanceName)

                instanceInformation.postSafely(info)
            }
        }
    }
}
package io.github.koss.mammut.feature.instancebrowser.recyclerview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.instances.InstanceDetail
import io.github.koss.mammut.repo.InstancesRepository
import io.github.koss.mammut.extension.postSafely
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class InstanceCardViewModel @Inject constructor(
    private val instancesRepository: InstancesRepository
) : ViewModel(), CoroutineScope by GlobalScope {

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
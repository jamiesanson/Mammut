package io.github.koss.mammut.feature.instancebrowser.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.koss.mammut.instances.response.InstanceDetail
import io.github.koss.mammut.extension.postSafely

class InstanceAboutViewModel: ViewModel() {

    val details: LiveData<InstanceDetail> = MutableLiveData()

    val registrationId: LiveData<Long> = MutableLiveData()

    fun onDetailsChanged(details: InstanceDetail, registrationId: Long) {
        this.details.postSafely(details)
        this.registrationId.postSafely(registrationId)
    }
}
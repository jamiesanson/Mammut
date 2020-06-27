package io.github.koss.mammut.base.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NavigationEventBus {

    val events: LiveData<Event<NavigationEvent>> = MutableLiveData()

    fun sendEvent(event: NavigationEvent) {
        (events as MutableLiveData).value = Event(event)
    }
}
package io.github.jamiesanson.mammut.feature.joininstance

import androidx.lifecycle.ViewModel
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceModule
import io.github.koss.randux.utils.Store
import javax.inject.Inject
import javax.inject.Named

class JoinInstanceViewModel @Inject constructor(
        @Named(JoinInstanceModule.JOIN_INSTANCE_STORE)
        private val store: Store
): ViewModel() {

    init {
        store.subscribe {
            (store.getState() as? JoinInstanceState)
                    ?.let(::onStateChanged)
        }
    }

    private fun onStateChanged(newState: JoinInstanceState) {

    }

    fun onInstanceUrlChanged(newUrl: String) {

    }
}
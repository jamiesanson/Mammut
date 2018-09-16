package io.github.jamiesanson.mammut.feature.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.koss.randux.utils.State
import io.github.koss.randux.utils.Store

/**
 * Base ViewModel for [Store] usage
 */
abstract class BaseViewModel<ViewState>(
        protected val store: Store
): ViewModel() {

    init {
        store.subscribe {
            store.getState().let(::onStateChanged)
        }
    }

    val viewState: LiveData<ViewState> = MutableLiveData()

    val viewEvents: LiveData<Event<Any>> = MutableLiveData()

    private fun onStateChanged(state: State) {
        (viewState as MutableLiveData).postValue(onNewState(state))
    }

    abstract fun onNewState(state: State): ViewState

}
package io.github.koss.mammut.architecture

import androidx.lifecycle.LiveDataReactiveStreams
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.lang.RuntimeException

abstract class Model<ViewState>(initialViewState: ViewState) {

    // Internal backing field for the ViewState
    private val viewStateRx = BehaviorSubject.create<ViewState>()

    // Internal backing field for SideEffects
    private val sideEffectsEx = PublishSubject.create<SideEffect>()

    private val disposable: Disposable

    init {
        disposable = sideEffectsEx
                .scan(initialViewState) { state, sideEffect ->
                    @Suppress("UNCHECKED_CAST")
                    when (sideEffect) {
                        is Model<*>.SetViewStateSideEffect -> sideEffect.newState as ViewState
                        else -> handleSideEffect(state, sideEffect)
                    }
                }
                .distinctUntilChanged()
                .doOnError {
                    // Throw RuntimeError if handleSideEffect produces an exception
                    throw RuntimeException(it)
                }
                .subscribe(viewStateRx::onNext)
    }

    /**
     * ViewState observable by the View. This is all that should be exposed to the View.
     */
    val viewState = LiveDataReactiveStreams
            .fromPublisher(
                    viewStateRx
                            .toFlowable(BackpressureStrategy.BUFFER
                            )
            )

    /**
     * Function to be called by the ViewModel implementation to pass in a new SideEffect. This will
     * trigger a call to handleSideEffect to map the SideEffect into the ViewState
     */
    fun onSideEffect(sideEffect: SideEffect) {
        sideEffectsEx.onNext(sideEffect)
    }

    /**
     * Function for disposing the underlying Rx chain. This should never be needed, but adding it
     * just in case.
     */
    fun dispose() {
        disposable.dispose()
    }

    /**
     * Function to be called by the model implementation to pass new ViewState through to backing
     * field.
     */
    protected fun onNewViewState(viewState: ViewState) {
        sideEffectsEx.onNext(SetViewStateSideEffect(viewState))
    }

    /**
     * Function for handling side effects passed in to the model
     */
    protected abstract fun handleSideEffect(currentState: ViewState, sideEffect: SideEffect): ViewState

    /**
     * Private inner class allowing directly setting the ViewState to act as a side effect
     */
    private inner class SetViewStateSideEffect(
            val newState: ViewState
    ): SideEffect
}
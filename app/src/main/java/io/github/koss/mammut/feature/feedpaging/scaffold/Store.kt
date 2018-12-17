package io.github.koss.mammut.feature.feedpaging.scaffold

import kotlin.properties.Delegates

/**
 * Store which maintains state, exposing an observable state, via [observe], and services [Event]s
 * via the [send] function.
 *
 * The store ensures that the state it maintains is [Reducible]. This means that when an event is
 * dispatched to the store, the current state is able to be reduced with the incoming event to
 * potentially create a new [State]. The observers are called in the order they were added.
 *
 * @param initialState The initial state of the store
 */
class Store<State: Reducible<Event, State>, Event>(
        initialState: State
) {

    /**
     * Internal state which is observable via the Kotlin observable property delegate
     */
    private var internalState: State by Delegates.observable(initialState) { _, old, new ->
        observers.forEach { it.stateChanged(old, new) }
    }

    /**
     * List of observers maintained internally
     */
    private val observers = mutableListOf<StateObserver<State>>()

    /**
     * Publicly exposed state for use if state information is required outside of an observer
     */
    val state: State
        get() = internalState

    /**
     * Function for adding a state observer
     *
     * @param observer The observer to add
     */
    fun observe(observer: StateObserver<State>) {
        observer.stateChanged(oldState = null, newState = state)
        observers.add(observer)
    }

    /**
     * Function for removing a state observer
     *
     * @param observer The observe to remove
     */
    fun removeObserver(observer: StateObserver<State>) {
        observers.remove(observer)
    }

    /**
     * Function for sending an event to the store
     *
     * @param event The event to be used in state reduction
     */
    fun send(event: Event) {
        internalState = state.reduce(event)
    }

    /**
     * Function for allowing state restoration
     *
     * @param state The state saved previously
     */
    fun restoreState(state: State) {
        internalState = state
    }
}
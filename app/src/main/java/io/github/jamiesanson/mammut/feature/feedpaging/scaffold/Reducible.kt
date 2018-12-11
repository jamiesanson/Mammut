package io.github.jamiesanson.mammut.feature.feedpaging.scaffold

interface Reducible<in Event, out State> {
    fun reduce(event: Event): State
}
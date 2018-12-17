package io.github.koss.mammut.feature.feedpaging.scaffold

interface Reducible<in Event, out State> {
    fun reduce(event: Event): State
}
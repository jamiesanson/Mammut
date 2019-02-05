package io.github.koss.mammut.architecture

/**
 * Marker interface for a SideEffect.
 * This should be used for things like Loading state and errors. Side effects should
 * be events which arise outside of the normal flow of data.
 */
interface SideEffect
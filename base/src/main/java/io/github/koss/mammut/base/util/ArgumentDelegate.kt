package io.github.koss.mammut.base.util

import com.bluelinelabs.conductor.Controller
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Simple delegate for making controller argument fetching nicer
 */
fun <T: Any?> arg(name: String) = object: ReadOnlyProperty<Controller, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Controller, property: KProperty<*>): T =
            thisRef.args.get(name) as T
}
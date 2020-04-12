package io.github.koss.mammut.base.util

import androidx.fragment.app.Fragment
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


/**
 * Simple delegate for making controller argument fetching nicer
 */
fun <T: Any?> Fragment.arg(name: String) = object: ReadOnlyProperty<Fragment, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T =
            thisRef.requireArguments().get(name) as T
}
package io.github.jamiesanson.mammut.component.retention

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.BaseController
import kotlin.reflect.KProperty

/**
 * This class handles retention of any object using the ViewModel framework. An
 * example of this method being used can be found in the Loader Manager framework
 * in the Android support library.
 */
class RetentionDelegate<T>(
        private val viewModelStoreOwnerGetter: () -> ViewModelStoreOwner,
        private val key: () -> String? = { null },
        private val initializer: (() -> T)? = null
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val (uniqueId, viewModel) = storeInformationFor(property)

        val key = this.key() ?: uniqueId

        // If the value is in the ViewModel, return it
        @Suppress("UNCHECKED_CAST")
        if (viewModel.values.containsKey(key)) return viewModel.values[key] as T

        // If the viewModel doesn't hold the value and the initialiser is null, we're in an illegal state
        initializer ?: throw IllegalStateException("Attempting to invoke getValue when no value is present")

        val value = initializer.invoke() // Note - Smartcast doesn't work when using a regular function call

        // Add the value to the ViewModel
        viewModel.values[key] = value as Any

        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val (uniqueId, viewModel) = storeInformationFor(property)

        val key = this.key() ?: uniqueId

        // Add the value to the ViewModel
        viewModel.values[key] = value as Any
    }

    /**
     * Convenience getter function for viewModel
     */
    private fun storeInformationFor(property: KProperty<*>): Pair<String, RetentionViewModel> {
        // Get ViewModel
        val viewModel = ViewModelProvider(viewModelStoreOwnerGetter(), FACTORY)[RetentionViewModel::class.java]

        // Use the property as a unique ID for the viewModel value map
        val uniqueId = property.toString() + property.hashCode()

        return uniqueId to viewModel
    }

    companion object {
        @JvmField
        val FACTORY = object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                    RetentionViewModel(mutableMapOf()) as T
        }
    }
}

/**
 * ViewModel class for holding the values to be retained in the viewModelStore
 */
class RetentionViewModel(
        var values: MutableMap<String, Any>
): ViewModel()

/**
 * Extension properties for ease of use
 */
fun <T> Fragment.retained(valInitializer: (() -> T)? = null) = RetentionDelegate({ this }, { null }, valInitializer)

fun <T> AppCompatActivity.retained(valInitializer: (() -> T)? = null) = RetentionDelegate({ this }, { null }, valInitializer)

// Controller stuff
fun <T> BaseController.retained(key: () -> String, valInitializer: (() -> T)? = null) = RetentionDelegate({ activity as ViewModelStoreOwner }, key, valInitializer)
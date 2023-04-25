package io.github.koss.mammut.base.util

import android.app.Activity
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import io.github.koss.mammut.base.R
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.navigation.NavigationHub
import io.github.koss.mammut.base.photoviewer.FullScreenPhotoViewer
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified T: ViewModel> AppCompatActivity.provideViewModel(viewModelFactory: MammutViewModelFactory): T =
        ViewModelProvider(this, viewModelFactory)[T::class.java]

fun Activity.snackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
    val typedValue = TypedValue()
    theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
    @ColorInt val primaryDarkColor = typedValue.data

    theme.resolveAttribute(R.attr.colorOnPrimary, typedValue, true)
    @ColorInt val lightAccentColor = typedValue.data

    Snackbar.make(window.decorView, message, length).apply {
        view.background.setTint(primaryDarkColor)
        view.findViewById<TextView>(R.id.snackbar_text).setTextColor(lightAccentColor)
    }.show()
}

@Suppress("UNCHECKED_CAST")
fun <T: CoordinatorLayout.Behavior<View>> View.behaviour(): T? =
        (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? T

/**
 * Helper function for allowing simple ViewHolder view inflation
 */
fun ViewGroup.inflate(@LayoutRes resource: Int, addToRoot: Boolean = false): View =
        LayoutInflater.from(context).inflate(resource, this, addToRoot)


fun <T> Fragment.viewLifecycleLazy(initialise: () -> T): ReadOnlyProperty<Fragment, T> =
        object : ReadOnlyProperty<Fragment, T>, DefaultLifecycleObserver {

            // A backing property to hold our value
            private var binding: T? = null

            private var viewLifecycleOwner: LifecycleOwner? = null

            init {
                // Observe the View Lifecycle of the Fragment
                this@viewLifecycleLazy
                        .viewLifecycleOwnerLiveData
                        .observe(this@viewLifecycleLazy, Observer { newLifecycleOwner ->
                            viewLifecycleOwner
                                    ?.lifecycle
                                    ?.removeObserver(this)

                            viewLifecycleOwner = newLifecycleOwner.also {
                                it.lifecycle.addObserver(this)
                            }
                        })
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                binding = null
            }

            override fun getValue(
                    thisRef: Fragment,
                    property: KProperty<*>
            ): T {
                // Return the backing property if it's set, or initialise
                return this.binding ?: initialise().also {
                    this.binding = it
                }
            }
        }

fun Fragment.findFullScreenPhotoViewer(): FullScreenPhotoViewer =
    (parentFragment as? FullScreenPhotoViewer)
        ?: parentFragment?.findFullScreenPhotoViewer()
        ?: /* Try the Activity */ requireActivity() as? FullScreenPhotoViewer
        ?: throw IllegalStateException("No parent SubComponentFactory found")

fun Fragment.findSubcomponentFactory(): SubcomponentFactory =
        (parentFragment as? SubcomponentFactory)
                ?: parentFragment?.findSubcomponentFactory()
                ?: /* Try the Activity */ requireActivity() as? SubcomponentFactory
                ?: throw IllegalStateException("No parent SubComponentFactory found")

fun Fragment.findNavigationHub(): NavigationHub =
        (parentFragment as? NavigationHub)
                ?: parentFragment?.findNavigationHub()
                ?: /* Try the Activity */ requireActivity() as? NavigationHub
                ?: throw IllegalStateException("No parent SubComponentFactory found")
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import io.github.koss.mammut.base.R
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import org.jetbrains.anko.contentView
import org.jetbrains.anko.textColor

inline fun <reified T: ViewModel> AppCompatActivity.provideViewModel(viewModelFactory: MammutViewModelFactory): T =
        ViewModelProviders.of(this, viewModelFactory)[T::class.java]

fun Activity.snackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
    val typedValue = TypedValue()
    theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
    @ColorInt val primaryDarkColor = typedValue.data

    theme.resolveAttribute(R.attr.colorAccentLight, typedValue, true)
    @ColorInt val lightAccentColor = typedValue.data

    Snackbar.make(contentView!!, message, length).apply {
        view.background.setTint(primaryDarkColor)
        view.findViewById<TextView>(R.id.snackbar_text).textColor = lightAccentColor
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
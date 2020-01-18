package io.github.koss.mammut.base.util

import android.content.Intent
import com.bluelinelabs.conductor.Controller
import com.google.android.material.snackbar.Snackbar
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import java.lang.IllegalStateException

fun Controller.snackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
    activity!!.snackbar(message, length)
}

inline fun <reified T> Controller.startActivity(finishCurrent: Boolean = false) =
        startActivity(Intent(activity, T::class.java)).also {
            if (finishCurrent) this@startActivity.activity?.finish()
        }

fun Controller.findSubcomponentFactory(): SubcomponentFactory =
    (parentController as? SubcomponentFactory)
        ?: parentController?.findSubcomponentFactory()
        ?: throw IllegalStateException("No parent SubComponentFactory found")

fun Controller.comingSoon() =
        snackbar("This feature is coming soon")

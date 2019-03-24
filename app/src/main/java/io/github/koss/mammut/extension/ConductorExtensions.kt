package io.github.koss.mammut.extension

import android.content.Intent
import com.bluelinelabs.conductor.Controller
import com.google.android.material.snackbar.Snackbar
import io.github.koss.mammut.feature.instance.dagger.InstanceComponent
import io.github.koss.mammut.feature.instance.subfeature.navigation.InstanceController
import java.lang.IllegalStateException


fun Controller.snackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
    activity!!.snackbar(message, length)
}

inline fun <reified T> Controller.startActivity(finishCurrent: Boolean = false) =
        startActivity(Intent(activity, T::class.java)).also {
            if (finishCurrent) this@startActivity.activity?.finish()
        }

fun Controller.comingSoon() =
        snackbar("This feature is coming soon")

fun Controller.instanceComponent(): InstanceComponent =
        (parentController as? InstanceController)?.component
                ?: parentController?.instanceComponent()
                ?: throw IllegalStateException("No parent InstanceController found")
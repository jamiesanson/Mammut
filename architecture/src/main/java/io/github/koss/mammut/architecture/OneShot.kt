package io.github.koss.mammut.architecture

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

/**
 * OneShot implementation used for ViewState events. These can be things like one-off messages which
 * should only be shown once, therefore the shown/consumed state must be kept track of.
 */
@Parcelize
class OneShot<T>(
        private val content: @RawValue T?,
        private var hasBeenHandled: Boolean = false
): Parcelable {

    fun hasBeenHandled(): Boolean = hasBeenHandled

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content!!

    fun isEmpty(): Boolean = content == null

    companion object {
        fun <T> empty() = OneShot<T>(null)
    }
}
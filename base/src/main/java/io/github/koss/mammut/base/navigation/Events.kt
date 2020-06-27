package io.github.koss.mammut.base.navigation

import io.github.koss.mammut.data.models.InstanceRegistration
import io.github.koss.mammut.data.models.domain.FeedType

// TODO - This whole file could do with a better solution
enum class Tab {
    Feed,
    Search,
    Notifications
}

sealed class NavigationEvent {
    sealed class Feed {
        data class TypeChanged(val targetInstanceToken: String, val newFeedType: FeedType): NavigationEvent()
        data class OffscreenCountChanged(val targetInstanceToken: String, val newCount: Int): NavigationEvent()
    }

    sealed class Instance {
        data class Changed(val newInstance: InstanceRegistration): NavigationEvent()
        data class TabReselected(val tab: Tab): NavigationEvent()
    }
}

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
data class Event<out T>(
        private val content: T
) {

    var hasBeenHandled = false
        private set // Allow external read but not write

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
    fun peekContent(): T = content
}

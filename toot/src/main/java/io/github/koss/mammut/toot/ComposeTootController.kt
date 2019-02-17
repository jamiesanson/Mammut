package io.github.koss.mammut.toot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.koss.mammut.base.BaseController
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions

/**
 * This is the main controller for composing a toot. All things related to how this controller operates
 * can be found in the Toot module.
 */
@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class ComposeTootController: BaseController() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
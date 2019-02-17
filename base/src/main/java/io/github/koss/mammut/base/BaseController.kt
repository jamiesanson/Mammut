package io.github.koss.mammut.base

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import com.bluelinelabs.conductor.archlifecycle.LifecycleController
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.coroutines.CoroutineContext

abstract class BaseController: LifecycleController, LayoutContainer, CoroutineScope {

    constructor(): super()

    constructor(args: Bundle): super(args)

    override val containerView: View?
        get() = view

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    private var restoreCalled: Boolean = false

    /**
     * This is called when containerView is valid
     */
    open fun initialise(savedInstanceState: Bundle?) {}

    @CallSuper
    override fun onAttach(view: View) {
        super.onAttach(view)
        if (!restoreCalled) {
            initialise(null)
        }
    }

    @CallSuper
    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        initialise(savedViewState)
        restoreCalled = true
    }
}
package io.github.koss.mammut.base

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import io.github.koss.mammut.base.themes.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject

/**
 * Base activity for theming (and more)
 */
abstract class BaseActivity: AppCompatActivity(), CoroutineScope by GlobalScope {

    @Inject
    lateinit var themeEngine: ThemeEngine

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        injectDependencies()
        themeEngine.apply(this)
        super.onCreate(savedInstanceState)
    }

    @CallSuper
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Enable edge-to-edge drawing
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    abstract fun injectDependencies()
}
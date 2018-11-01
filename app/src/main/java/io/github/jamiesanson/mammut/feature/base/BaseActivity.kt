package io.github.jamiesanson.mammut.feature.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.feature.themes.ThemeEngine
import org.jetbrains.anko.attr
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.contentView
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import javax.inject.Inject

/**
 * Base activity for theming (and more)
 */
abstract class BaseActivity: AppCompatActivity() {

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            contentView?.apply {
                systemUiVisibility = when {
                    themeEngine.isLightTheme -> View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    else -> systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    abstract fun injectDependencies()
}
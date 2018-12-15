package io.github.jamiesanson.mammut.feature.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.feature.themes.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.jetbrains.anko.attr
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.contentView
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            contentView?.apply {
                systemUiVisibility = when {
                    themeEngine.isLightTheme -> View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            this or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        } else {
                            this
                        }
                    }
                    else -> systemUiVisibility and (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            this or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        } else {
                            this
                        }
                    }).inv()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Only apply this on O such that we can also tint the nav bar icons
            window.navigationBarColor = colorAttr(R.attr.colorPrimary)
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    abstract fun injectDependencies()
}
package io.github.koss.mammut

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import io.fabric.sdk.android.Fabric
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.component.util.Blurrer
import io.github.koss.mammut.dagger.application.ApplicationComponent
import io.github.koss.mammut.dagger.application.ApplicationModule
import io.github.koss.mammut.dagger.application.DaggerApplicationComponent
import javax.inject.Inject
import saschpe.android.customtabs.CustomTabsActivityLifecycleCallbacks

class MammutApplication: Application() {

    lateinit var component: ApplicationComponent

    @Inject
    lateinit var themeEngine: ThemeEngine

    override fun onCreate() {
        super.onCreate()
        component = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
                .also { it.inject(this) }

        themeEngine.updateFontDefaults()

        AndroidThreeTen.init(this)

        Blurrer.initialise(this)

        Fabric.with(this, Crashlytics())

        // Preload custom tabs
        registerActivityLifecycleCallbacks(CustomTabsActivityLifecycleCallbacks())
    }
}
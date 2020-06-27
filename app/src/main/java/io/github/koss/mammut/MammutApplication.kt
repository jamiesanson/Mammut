package io.github.koss.mammut

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.crashlytics.android.Crashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import io.fabric.sdk.android.Fabric
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.dagger.application.ApplicationComponent
import io.github.koss.mammut.dagger.module.ApplicationModule
import io.github.koss.mammut.dagger.application.DaggerApplicationComponent
import javax.inject.Inject
import saschpe.android.customtabs.CustomTabsActivityLifecycleCallbacks

class MammutApplication: Application() {

    lateinit var component: ApplicationComponent

    @Inject
    lateinit var themeEngine: ThemeEngine

    @Inject
    lateinit var workerFactory: WorkerFactory

    override fun onCreate() {
        super.onCreate()
        component = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
                .also { it.inject(this) }

        AndroidThreeTen.init(this)

        Fabric.with(this, Crashlytics())

        // Preload custom tabs
        registerActivityLifecycleCallbacks(CustomTabsActivityLifecycleCallbacks())

        // Initialise WorkManager
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())
    }
}
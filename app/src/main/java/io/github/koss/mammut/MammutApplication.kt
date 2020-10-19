package io.github.koss.mammut

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.jakewharton.threetenabp.AndroidThreeTen
import io.github.koss.mammut.base.themes.ThemeEngine
import io.github.koss.mammut.base.util.Logger
import io.github.koss.mammut.base.util.Logging
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
        // Initialise logging - log to Crashlytics if not debuggable
        if (!BuildConfig.DEBUG) {
            Logging.loggers.add(CrashlyticsLogger)
        }

        // Dependencies
        component = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
                .also { it.inject(this) }

        AndroidThreeTen.init(this)

        // Preload custom tabs
        registerActivityLifecycleCallbacks(CustomTabsActivityLifecycleCallbacks())

        // Initialise WorkManager
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())
    }
}

private val CrashlyticsLogger = Logger { priority, tag, message, throwable ->
    // Format: E/TAG: my message
    Firebase.crashlytics.log("${priority.name.first()}/$tag: ${message()}")

    // Log the exception
    if (throwable != null) {
        Firebase.crashlytics.recordException(throwable)
    }
}
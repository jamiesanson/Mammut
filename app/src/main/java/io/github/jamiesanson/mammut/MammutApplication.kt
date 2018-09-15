package io.github.jamiesanson.mammut

import android.app.Application
import io.github.jamiesanson.mammut.dagger.application.ApplicationComponent
import io.github.jamiesanson.mammut.dagger.application.ApplicationModule
import io.github.jamiesanson.mammut.dagger.application.DaggerApplicationComponent
import io.github.jamiesanson.mammut.feature.themes.ThemeEngine
import javax.inject.Inject

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
    }
}
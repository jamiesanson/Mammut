package io.github.jamiesanson.mammut

import android.app.Application
import io.github.jamiesanson.mammut.dagger.application.ApplicationComponent
import io.github.jamiesanson.mammut.dagger.application.DaggerApplicationComponent

class MammutApplication: Application() {

    lateinit var component: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        component = DaggerApplicationComponent.builder()
                .build()
    }
}
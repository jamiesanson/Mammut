package io.github.koss.mammut.extension

import androidx.appcompat.app.AppCompatActivity
import io.github.koss.mammut.MammutApplication
import io.github.koss.mammut.dagger.application.ApplicationComponent

val AppCompatActivity.mammutApplication: MammutApplication
    get() = application as MammutApplication

val AppCompatActivity.applicationComponent: ApplicationComponent
    get() = mammutApplication.component

package io.github.jamiesanson.mammut.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import io.github.jamiesanson.mammut.MammutApplication
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.dagger.application.ApplicationComponent

val AppCompatActivity.mammutApplication: MammutApplication
    get() = application as MammutApplication

val AppCompatActivity.applicationComponent: ApplicationComponent
    get() = mammutApplication.component

inline fun <reified T: ViewModel> AppCompatActivity.provideViewModel(viewModelFactory: MammutViewModelFactory): T =
        ViewModelProviders.of(this, viewModelFactory)[T::class.java]
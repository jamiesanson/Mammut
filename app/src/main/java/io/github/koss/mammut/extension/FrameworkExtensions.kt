package io.github.koss.mammut.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import io.github.koss.mammut.MammutApplication
import io.github.koss.mammut.dagger.application.ApplicationComponent

val FragmentActivity.mammutApplication: MammutApplication
    get() = application as MammutApplication

val FragmentActivity.applicationComponent: ApplicationComponent
    get() = mammutApplication.component

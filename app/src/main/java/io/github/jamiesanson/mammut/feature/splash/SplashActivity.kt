package io.github.jamiesanson.mammut.feature.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.jamiesanson.mammut.feature.joininstance.JoinInstanceActivity
import org.jetbrains.anko.startActivity

class SplashActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity<JoinInstanceActivity>()
        finish()
    }
}
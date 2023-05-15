package io.github.koss.mammut.feature.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.koss.mammut.repo.RegistrationRepository
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.multiinstance.MultiInstanceActivity
import io.github.koss.mammut.feature.joininstance.JoinInstanceActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

class SplashActivity : AppCompatActivity(), CoroutineScope by GlobalScope {

    @Inject
    lateinit var registrationRepository: RegistrationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.inject(this)

        launch {
            val completedRegistrations = registrationRepository
                    .getAllRegistrations()
                    .filter { it.account != null }

            withContext(Dispatchers.Main) {
                if (completedRegistrations.isNotEmpty()) {
                    startActivity(Intent(this@SplashActivity, MultiInstanceActivity::class.java))
                } else {
                    startActivity(Intent(this@SplashActivity, JoinInstanceActivity::class.java))
                }

                finish()
            }
        }
    }
}
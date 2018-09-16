package io.github.jamiesanson.mammut.feature.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.jamiesanson.mammut.data.repo.RegistrationRepository
import io.github.jamiesanson.mammut.extension.applicationComponent
import io.github.jamiesanson.mammut.feature.instancebrowser.InstanceBrowserActivity
import io.github.jamiesanson.mammut.feature.joininstance.JoinInstanceActivity
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.jetbrains.anko.startActivity
import javax.inject.Inject

class SplashActivity: AppCompatActivity() {

    @Inject lateinit var registrationRepository: RegistrationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.inject(this)

        launch {
            val hasRegistrations = registrationRepository.hasRegistrations()

            withContext(UI) {
                if (hasRegistrations) {
                    startActivity<InstanceBrowserActivity>()
                } else {
                    startActivity<JoinInstanceActivity>()
                }

                finish()
            }
        }
    }
}
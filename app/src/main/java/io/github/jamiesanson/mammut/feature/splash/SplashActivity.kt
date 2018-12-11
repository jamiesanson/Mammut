package io.github.jamiesanson.mammut.feature.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.jamiesanson.mammut.data.repo.PreferencesRepository
import io.github.jamiesanson.mammut.data.repo.RegistrationRepository
import io.github.jamiesanson.mammut.extension.applicationComponent
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instancebrowser.InstanceBrowserActivity
import io.github.jamiesanson.mammut.feature.joininstance.JoinInstanceActivity
import kotlinx.coroutines.*
import org.jetbrains.anko.startActivity
import javax.inject.Inject

class SplashActivity : AppCompatActivity(), CoroutineScope by GlobalScope {

    @Inject
    lateinit var registrationRepository: RegistrationRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.inject(this)
        if (preferencesRepository.takeMeStraightToInstanceBrowser) {
            startActivity<InstanceBrowserActivity>()
            finish()
            return
        }

        launch {
            val registrations = registrationRepository
                    .getAllRegistrations()

            withContext(Dispatchers.Main) {
                registrations.forEach {
                    it.run {
                        when {
                            accessToken != null && accessToken.accessToken == preferencesRepository.lastAccessedInstanceToken -> {
                                InstanceActivity.launch(this@SplashActivity,
                                        instanceName = instanceName,
                                        authCode = accessToken.accessToken)
                                finish()
                                return@withContext
                            }
                            else -> {
                                // no-op
                            }
                        }
                    }
                }

                startActivity<JoinInstanceActivity>()
                finish()
            }
        }
    }
}
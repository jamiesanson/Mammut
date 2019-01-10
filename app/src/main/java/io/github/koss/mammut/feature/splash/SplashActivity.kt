package io.github.koss.mammut.feature.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.koss.mammut.data.repo.PreferencesRepository
import io.github.koss.mammut.data.repo.RegistrationRepository
import io.github.koss.mammut.extension.applicationComponent
import io.github.koss.mammut.feature.instance.InstanceActivity
import io.github.koss.mammut.feature.instancebrowser.InstanceBrowserActivity
import io.github.koss.mammut.feature.joininstance.JoinInstanceActivity
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
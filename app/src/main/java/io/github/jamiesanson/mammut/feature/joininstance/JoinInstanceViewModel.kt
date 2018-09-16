package io.github.jamiesanson.mammut.feature.joininstance

import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.method.Apps
import com.sys1yagi.mastodon4j.api.method.Public
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.data.models.InstanceAccessToken
import io.github.jamiesanson.mammut.data.models.InstanceRegistration
import io.github.jamiesanson.mammut.data.repo.PreferencesRepository
import io.github.jamiesanson.mammut.data.repo.RegistrationRepository
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.extension.run
import io.github.jamiesanson.mammut.feature.base.Event
import io.github.jamiesanson.mammut.feature.base.InputError
import kotlinx.coroutines.experimental.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

class JoinInstanceViewModel @Inject constructor(
        private val registrationRepository: RegistrationRepository,
        private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val isLoading: LiveData<Boolean> = MutableLiveData()

    val errorMessage: LiveData<Event<(Resources) -> String>> = MutableLiveData()

    val oauthUrl: LiveData<Event<String>> = MutableLiveData()

    val registrationCompleteEvent: LiveData<Event<Unit?>> = MutableLiveData()

    fun login(dirtyUrl: String, redirectUrl: String, clientName: String) {
        launch {
            isLoading.postSafely(true)

            val url = dirtyUrl.canonicalize()

            val client = getClientForUrl(url)
            val apps = Apps(client)

            // Attempt to make a call to get the instance info - if it fails, the URL is incorrect
            // and we shouldn't continue
            val instanceResult = Public(client).getInstance().run()
            if (instanceResult is Either.Left) {
                isLoading.postSafely(false)
                errorMessage.postSafely(InputError { resources -> resources.getString(R.string.error_non_existant_instance) })

                return@launch
            }

            val result = apps.createApp(
                    clientName = clientName,
                    redirectUris = redirectUrl,
                    scope = Scope(Scope.Name.ALL)).run()

            val registration = when (result) {
                is Either.Right -> {
                    result.b
                }
                is Either.Left -> {
                    isLoading.postSafely(false)
                    errorMessage.postSafely(Event({ _ -> result.a }))

                    return@launch
                }
            }

            val clientId = registration.clientId

            val oauthUrl = apps.getOAuthUrl(clientId, Scope(Scope.Name.ALL), redirectUrl)

            // Save the partial registration here for use when returning
            registrationRepository.addOrUpdateRegistration(InstanceRegistration(
                    id = registration.id,
                    clientId = registration.clientId,
                    clientSecret = registration.clientSecret,
                    redirectUri = redirectUrl,
                    instanceName = registration.instanceName
            ))

            // Save the URL for later
            preferencesRepository.loginDomain = url

            this@JoinInstanceViewModel.oauthUrl.postSafely(Event(oauthUrl))
        }
    }

    fun finishLogin(uri: Uri) {
        isLoading.postSafely(true)

        launch {
            // This should either have returned an authorization code or an error.
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            val instanceName = preferencesRepository.loginDomain ?: ""

            val registration = registrationRepository.getRegistrationForName(instanceName)

            if (error != null || code == null || registration == null) {
                isLoading.postSafely(false)
                errorMessage.postSafely(Event({ resources -> resources.getString(R.string.error_generic) }))
                return@launch
            }

            val client = getClientForUrl(instanceName)

            val apps = Apps(client)

            val result = apps.getAccessToken(
                    clientId = registration.clientId,
                    clientSecret = registration.clientSecret,
                    redirectUri = registration.redirectUri,
                    code = code,
                    grantType = "authorization_code"
            ).run()

            val accessToken = when (result) {
                is Either.Right -> {
                    result.b
                }
                is Either.Left -> {
                    isLoading.postSafely(false)
                    errorMessage.postSafely(Event({ _ -> result.a }))

                    return@launch
                }
            }

            // Save access token with registration
            val completedRegistration = registration.copy(
                    accessToken = InstanceAccessToken(
                            accessToken = accessToken.accessToken,
                            tokenType = accessToken.tokenType,
                            scope = accessToken.scope,
                            createdAt = accessToken.createdAt
                    )
            )

            registrationRepository.addOrUpdateRegistration(completedRegistration)

            isLoading.postSafely(false)
            registrationCompleteEvent.postSafely(Event(null))
        }
    }

    private fun getClientForUrl(url: String): MastodonClient = MastodonClient.Builder(url, OkHttpClient.Builder(), Gson()).build()

    private fun String.canonicalize(): String {
        // Strip any schemes out.
        var s = replaceFirst("http://", "")
        s = s.replaceFirst("https://", "")
        // If a username was included (e.g. username@example.com), just take what's after the '@'.
        val at = s.lastIndexOf('@')
        if (at != -1) {
            s = s.substring(at + 1)
        }
        return s.trim { it <= ' ' }
    }
}
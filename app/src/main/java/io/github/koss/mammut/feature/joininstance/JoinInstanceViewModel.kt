package io.github.koss.mammut.feature.joininstance

import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.method.Accounts
import com.sys1yagi.mastodon4j.api.method.Apps
import com.sys1yagi.mastodon4j.api.method.Public
import io.github.koss.mammut.R
import io.github.koss.mammut.data.models.*
import io.github.koss.mammut.data.repository.InstancesRepository
import io.github.koss.mammut.repo.PreferencesRepository
import io.github.koss.mammut.repo.RegistrationRepository
import io.github.koss.mammut.extension.ClientBuilder
import io.github.koss.mammut.extension.postSafely
import io.github.koss.mammut.data.extensions.run
import io.github.koss.mammut.feature.base.Event
import io.github.koss.mammut.feature.base.InputError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class JoinInstanceViewModel @Inject constructor(
        private val registrationRepository: RegistrationRepository,
        private val preferencesRepository: PreferencesRepository,
        private val instancesRepository: InstancesRepository,
        private val clientBuilder: ClientBuilder
) : ViewModel(), CoroutineScope by GlobalScope {

    val isLoading: LiveData<Boolean> = MutableLiveData()

    val errorMessage: LiveData<Event<(Resources) -> String>> = MutableLiveData()

    val oauthUrl: LiveData<Event<String>> = MutableLiveData()

    val registrationCompleteEvent: LiveData<Event<Unit?>> = MutableLiveData()

    val searchResults: LiveData<List<InstanceSearchResult>> = MutableLiveData()

    init {
        launch {
            instancesRepository.initialiseResults()
        }
    }

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
                    errorMessage.postSafely(Event({ _ -> result.a.description }))

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

    fun onQueryChanged(query: String) {
        launch {
            val results = instancesRepository.searchInstances(query)
            searchResults.postSafely(results)
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
                    errorMessage.postSafely(Event({ _ -> result.a.description }))

                    return@launch
                }
            }

            // Get the users account and save it with the registration
            val authenticatedClient = getClientForUrl(instanceName, accessToken.accessToken)
            val accountResult = Accounts(authenticatedClient).getVerifyCredentials().run()

            val account = when (accountResult) {
                is Either.Right -> accountResult.b
                is Either.Left -> {
                    isLoading.postSafely(false)
                    errorMessage.postSafely(Event({ _ -> accountResult.a.description }))

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
                    ),
                    account = account.run {
                        Account(
                                id,
                                userName,
                                acct,
                                displayName,
                                note,
                                url,
                                avatar,
                                header,
                                isLocked,
                                createdAt,
                                followersCount,
                                followingCount,
                                statusesCount,
                                ArrayList(emojis.map { it.run { Emoji(shortcode, staticUrl, url, visibleInPicker) } })
                        )
                    }
            )

            // Update the registration repo
            registrationRepository.addOrUpdateRegistration(completedRegistration)

            isLoading.postSafely(false)
            registrationCompleteEvent.postSafely(Event(null))
        }
    }

    private fun getClientForUrl(url: String, accessToken: String? = null): MastodonClient = clientBuilder.getInstanceBuilder(url)
            .run {
                accessToken?.let {
                    accessToken(it)
                } ?: this
            }
            .build()

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
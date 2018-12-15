package io.github.jamiesanson.mammut.data.models

/**
 * Model describing an instance registration.
 */
data class InstanceRegistration(
        val id: Long = 0,
        val clientId: String = "",
        val clientSecret: String = "",
        val redirectUri: String = "",
        val instanceName: String = "",
        val accessToken: InstanceAccessToken? = null,
        val account: Account? = null
)
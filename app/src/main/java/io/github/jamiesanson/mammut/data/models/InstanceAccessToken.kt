package io.github.jamiesanson.mammut.data.models

data class InstanceAccessToken(
        val accessToken: String = "",
        val tokenType: String = "",
        val scope: String = "",
        val createdAt: Long = 0
)
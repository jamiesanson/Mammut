package io.github.koss.mammut.extension

import com.google.gson.GsonBuilder
import com.sys1yagi.mastodon4j.MastodonClient
import io.github.koss.mammut.data.models.Account
import okhttp3.OkHttpClient

class ClientBuilder(
        private val okHttpClient: OkHttpClient.Builder,
        private val gson: GsonBuilder
) {

    fun getInstanceBuilder(instanceName: String): MastodonClient.Builder =
            MastodonClient.Builder(instanceName, okHttpClient, gson)
}

fun Account.fullAcct(instanceName: String): String = "@$userName@$instanceName"
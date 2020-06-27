package io.github.koss.mammut.data.extensions

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException
import io.github.koss.mammut.data.BuildConfig
import io.github.koss.mammut.data.models.Account
import okhttp3.OkHttpClient

sealed class Result<T> {
    data class Success<T>(val data: T): Result<T>()
    data class Failure<T>(val error: Error): Result<T>()
}

fun <T> Result<T>.orNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Failure -> null
}

tailrec suspend fun <T> MastodonRequest<T>.run(retryCount: Int = 0): Result<T> {
    val result = try {
        Result.Success(execute())
    } catch (e: Mastodon4jRequestException) {
        if (e.isErrorResponse()) {
            e.response?.body()?.string()?.run {
                when {
                    startsWith("{") ->  try {
                        Result.Failure<T>(GsonBuilder()
                                .excludeFieldsWithoutExposeAnnotation()
                                .create()
                                .fromJson(e.response?.body()?.charStream(), Error::class.java))
                    } catch (e: Exception) {
                        null
                    }
                    isNotEmpty() -> unknownError<T>(this)
                    else -> null
                }
            } ?: unknownError<T>(if (BuildConfig.DEBUG) "Exception from non-error response" else "Oh jeez, something's gone wrong")
        } else {
            unknownError<T>(if (BuildConfig.DEBUG) "Exception from non-error response" else "Oh jeez, something's gone wrong")
        }
    }

    if (result is Result.Failure && retryCount > 0) {
        return run(retryCount - 1)
    }

    if (result is Result.Failure) {
        Log.e("MastodonRunner", "An error occurred: ${result.error}")
    }

    return result
}

@Suppress("BlockingMethodInNonBlockingContext")
private fun <T> unknownError(description: String) =
        Result.Failure<T>(Error(error = "unknown_error", description = description))

data class Error(
        @Expose
        val error: String,
        @Expose
        @SerializedName("error_description")
        val description: String
)

fun Account.fullAcct(instanceName: String): String = "@$userName@$instanceName"


class ClientBuilder(
        private val okHttpClient: OkHttpClient.Builder,
        private val gson: GsonBuilder
) {

    fun getInstanceBuilder(instanceName: String): MastodonClient.Builder =
            MastodonClient.Builder(instanceName, okHttpClient, gson)
}

package io.github.jamiesanson.mammut.extension

import android.util.Log
import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException
import io.github.jamiesanson.mammut.BuildConfig
import okhttp3.OkHttpClient

tailrec suspend fun <T> MastodonRequest<T>.run(retryCount: Int = 0): Either<Error, T> {
    val result = try {
        Right(execute())
    } catch (e: Mastodon4jRequestException) {
        Log.e("MastodonRunner", "An error occurred", e)
        if (e.isErrorResponse()) {
            e.response?.body()?.string()?.run {
                when {
                    startsWith("{") ->  Left(GsonBuilder()
                            .excludeFieldsWithoutExposeAnnotation()
                            .create()
                            .fromJson<Error>(e.response?.body()?.charStream(), Error::class.java))
                    isNotEmpty() -> unknownError(this)
                    else -> null
                }
            } ?: unknownError(if (BuildConfig.DEBUG) "Exception from non-error response" else "Oh jeez, something's gone wrong")
        } else {
            unknownError(if (BuildConfig.DEBUG) "Exception from non-error response" else "Oh jeez, something's gone wrong")
        }
    }

    if (result is Either.Left && retryCount > 0) {
        return run(retryCount - 1)
    }

    return result
}

private fun unknownError(description: String) =
        Left(Error(error = "unknown_error", description = description))

data class Error(
        @Expose
        val error: String,
        @Expose
        @SerializedName("error_description")
        val description: String
)

class ClientBuilder(
        private val okHttpClient: OkHttpClient.Builder,
        private val gson: GsonBuilder
) {

    fun getInstanceBuilder(instanceName: String): MastodonClient.Builder =
            MastodonClient.Builder(instanceName, okHttpClient, gson)
}
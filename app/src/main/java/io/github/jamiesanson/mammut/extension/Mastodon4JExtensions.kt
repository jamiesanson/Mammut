package io.github.jamiesanson.mammut.extension

import android.util.Log
import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException
import io.github.jamiesanson.mammut.BuildConfig

fun <T> MastodonRequest<T>.run(): Either<String, T> = try {
    Right(execute())
} catch (e: Mastodon4jRequestException) {
    Log.e("MastodonRunner", "An error occurred", e)
    when {
        BuildConfig.DEBUG -> {
            if (e.isErrorResponse()) {
                val error = Gson().fromJson<Error>(e.response?.body()?.charStream(), Error::class.java)
                Log.e("MastodonRunner", "Error body: ${error.description}")
                Left("$error")
            } else {
                Left("Exception from non-error response")
            }
        }
        else -> {
            Left("Oh jeez, something's gone wrong")
        }
    }
}

private data class Error(
        val error: String,
        @SerializedName("error_description")
        val description: String
)
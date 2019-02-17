package io.github.koss.mammut.toot.repo

import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.entity.Emoji
import com.sys1yagi.mastodon4j.api.method.Public
import com.sys1yagi.mastodon4j.api.method.Statuses
import io.github.koss.mammut.base.extensions.run
import io.github.koss.mammut.toot.model.SubmissionState
import io.github.koss.mammut.toot.model.TootModel

/**
 * Class for handling status related network interactions
 */
class StatusRepository(private val client: MastodonClient) {

    suspend fun post(model: TootModel): SubmissionState {
        Statuses(client).postStatus(
                status = model.status,
                inReplyToId = model.inReplyToId,
                mediaIds = model.mediaIds,
                sensitive = model.sensitive,
                spoilerText = model.spoilerText,
                visibility = model.visibility
        ).run().let { result ->
            return when (result) {
                is Either.Left -> {
                    SubmissionState(
                            isSubmitting = false,
                            error = result.a.error
                    )
                }
                is Either.Right -> {
                    SubmissionState(
                            isSubmitting = false,
                            hasSubmitted = true
                    )
                }
            }
        }
    }

    suspend fun loadEmojis(): List<Emoji> {
        Public(client).getEmojis().run(retryCount = 2).let { result ->
            return when (result) {
                is Either.Left -> {
                    emptyList()
                }
                is Either.Right -> {
                    result.b
                }
            }
        }
    }
}
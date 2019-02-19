package io.github.koss.mammut.data.repository

import arrow.core.getOrElse
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.method.Public
import io.github.koss.mammut.base.extensions.run
import io.github.koss.mammut.data.converters.toEntity
import io.github.koss.mammut.data.database.MammutDatabase
import io.github.koss.mammut.data.database.entities.EmojiListEntity
import io.github.koss.mammut.data.models.Emoji
import java.util.*

private const val EMOJI_TIMEOUT_MS = 7 /* days */ * 24 * 60 * 60 * 1000

/**
 * Repository for interacting with the public API
 */
class InstanceDetailRepository(
        private val mammutDatabase: MammutDatabase,
        private val clientBuilder: (url: String) -> MastodonClient
) {

    /**
     * Function for loading emojis for a specific instance
     *
     * * Checks in DB first
     * * If found, return them
     * * Else call the API to retrieve them, then save them back to the db.
     *
     */
    suspend fun loadEmojisForInstance(instance: String): List<Emoji> {
        val emojiDetail = mammutDatabase.instanceDetailDao().getEmojisForUrl(instance)

        return when {
            emojiDetail != null && (Calendar.getInstance().timeInMillis - emojiDetail.retrievalTimeMs) < EMOJI_TIMEOUT_MS ->
                emojiDetail.emojis
            else -> Public(clientBuilder(instance)).getEmojis()
                    .run()
                    .getOrElse { emptyList() }
                    .map { it.toEntity() }
                    .also {
                        // Save to DB
                        mammutDatabase.instanceDetailDao()
                                .insertEmojis(EmojiListEntity(
                                        instance = instance,
                                        emojis = arrayListOf(*it.toTypedArray()),
                                        retrievalTimeMs = Calendar.getInstance().timeInMillis
                                ))
                    }
        }
    }
}
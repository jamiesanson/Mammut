package io.github.jamiesanson.mammut.data.converters

import com.sys1yagi.mastodon4j.api.entity.Status

fun Status.toEntity(): io.github.jamiesanson.mammut.data.database.entities.feed.Status =
        io.github.jamiesanson.mammut.data.database.entities.feed.Status(
                id,
                uri,
                url,
                account?.toEntity(),
                inReplyToId,
                inReplyToAccountId,
                reblog?.id ?: 0L,
                content,
                createdAt,
                ArrayList(emojis.map { it.toEntity() }),
                repliesCount,
                reblogsCount,
                favouritesCount,
                isReblogged,
                isFavourited,
                isSensitive,
                spoilerText,
                visibility
        )
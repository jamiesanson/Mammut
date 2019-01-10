package io.github.koss.mammut.data.converters

import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.data.models.Application
import io.github.koss.mammut.data.models.Mention
import io.github.koss.mammut.data.models.Tag

fun Status.toEntity(): io.github.koss.mammut.data.database.entities.feed.Status =
        io.github.koss.mammut.data.database.entities.feed.Status(
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
                visibility,
                ArrayList(mediaAttachments),
                ArrayList(mentions.map {
                        Mention(
                                it.url,
                                it.username,
                                it.acct,
                                it.id
                        )
                }),
                ArrayList(tags.map {
                        Tag(
                                it.name,
                                it.url
                        )
                }),
                application?.run {
                        Application(
                                name,
                                website
                        )
                },
                language,
                pinned
        )
package io.github.jamiesanson.mammut.data.converters

import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.jamiesanson.mammut.data.models.Application
import io.github.jamiesanson.mammut.data.models.Attachment
import io.github.jamiesanson.mammut.data.models.Mention
import io.github.jamiesanson.mammut.data.models.Tag

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
                visibility,
                ArrayList(mediaAttachments.map {
                        Attachment(
                                it.id,
                                it.type,
                                it.url,
                                it.remoteUrl,
                                it.previewUrl,
                                it.textUrl
                        )
                }),
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

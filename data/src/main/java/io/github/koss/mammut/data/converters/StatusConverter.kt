package io.github.koss.mammut.data.converters

import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.data.models.Application
import io.github.koss.mammut.data.models.Emoji
import io.github.koss.mammut.data.models.Mention
import io.github.koss.mammut.data.models.Tag

fun Status.toLocalModel(): io.github.koss.mammut.data.database.entities.feed.Status =
        io.github.koss.mammut.data.database.entities.feed.Status(
                id,
                uri,
                url,
                account?.toLocalModel(),
                inReplyToId,
                inReplyToAccountId,
                reblog?.id ?: 0L,
                content,
                createdAt,
                ArrayList(emojis.map { it.toLocalModel() }),
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

// WARNING: This is incomplete - it returns a status with simply the ID field filled out
fun io.github.koss.mammut.data.models.Status.toNetworkModel() : Status = Status(id)

fun io.github.koss.mammut.data.database.entities.feed.Status.toDomainModel(): io.github.koss.mammut.data.models.Status = io.github.koss.mammut.data.models.Status(
        id,
        uri,
        url,
        account,
        inReplyToId,
        inReplyToAccountId,
        reblogId,
        content,
        createdAt,
        ArrayList(emojis?.map { it.toDomainModel() } ?: emptyList()),
        repliesCount,
        reblogsCount,
        favouritesCount,
        isReblogged,
        isFavourited,
        isSensitive,
        spoilerText,
        visibility,
        mediaAttachments,
        mentions,
        tags,
        application,
        language,
        pinned
)
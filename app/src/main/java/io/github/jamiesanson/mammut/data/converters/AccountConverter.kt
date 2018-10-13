package io.github.jamiesanson.mammut.data.converters

import com.sys1yagi.mastodon4j.api.entity.Account

fun Account.toEntity(): io.github.jamiesanson.mammut.data.models.Account =
        io.github.jamiesanson.mammut.data.models.Account(id,
                userName, acct, displayName, note,
                url, avatar, header, isLocked, createdAt, followersCount, followingCount, statusesCount, emojis.map { it.toEntity() })
package io.github.koss.mammut.data.converters

import com.sys1yagi.mastodon4j.api.entity.Account

fun Account.toLocalModel(): io.github.koss.mammut.data.models.Account =
        io.github.koss.mammut.data.models.Account(id,
                userName, acct, displayName, note,
                url, avatar, header, isLocked, createdAt, followersCount, followingCount, statusesCount, ArrayList(emojis.map { it.toLocalModel() }))
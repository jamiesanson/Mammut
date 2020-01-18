package io.github.koss.mammut.data.converters

import io.github.koss.mammut.data.models.Emoji

fun com.sys1yagi.mastodon4j.api.entity.Emoji.toLocalModel(): Emoji =
        Emoji(shortcode, staticUrl, url, visibleInPicker)

fun Emoji.toNetworkModel(): com.sys1yagi.mastodon4j.api.entity.Emoji =
        com.sys1yagi.mastodon4j.api.entity.Emoji(shortcode, staticUrl, url, visibleInPicker)

fun Emoji.toDomainModel(): Emoji = this
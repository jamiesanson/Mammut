package io.github.koss.mammut.feed.domain.paging.network

import com.sys1yagi.mastodon4j.api.Handler
import com.sys1yagi.mastodon4j.api.entity.Notification
import com.sys1yagi.mastodon4j.api.entity.Status

class PublicStreamHandler(private val callback: (Status) -> Unit): Handler {
    override fun onDelete(id: Long) {}

    override fun onNotification(notification: Notification) {}

    override fun onStatus(status: Status) = callback(status)

}
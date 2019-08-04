package io.github.koss.mammut.feed.domain

import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.Handler
import com.sys1yagi.mastodon4j.api.Pageable
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.Shutdownable
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Accounts
import com.sys1yagi.mastodon4j.api.method.Public
import com.sys1yagi.mastodon4j.api.method.Streaming
import com.sys1yagi.mastodon4j.api.method.Timelines

private const val KEY_HOME_FEED = "home"
private const val KEY_LOCAL_FEED = "local"
private const val KEY_FEDERATED_FEED = "federated"
private const val KEY_ACCOUNT_FEED = "account_toots"

sealed class FeedType(
        val key: String,
        val persistenceEnabled: Boolean,
        val supportsStreaming: Boolean) {

    abstract fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>>

    abstract fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)?

    object Home: FeedType(KEY_HOME_FEED, true, false) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? = null

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Timelines(client)::getHome
    }

    object Local: FeedType(KEY_LOCAL_FEED, true, true) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? =
                Streaming(client)::localPublic

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Public(client)::getLocalPublic
    }

    object Federated: FeedType(KEY_FEDERATED_FEED, false, true) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? =
                Streaming(client)::federatedPublic

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Public(client)::getFederatedPublic
    }

    data class AccountToots(val accountId: Long, val withReplies: Boolean): FeedType("${KEY_ACCOUNT_FEED}_${accountId}_$withReplies", false, false) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? = null

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> {
            return {
                Accounts(client).getStatuses(
                        accountId = accountId,
                        onlyMedia = false,
                        excludeReplies = !withReplies,
                        pinned = false,
                        range = it
                )
            }
        }
    }
}
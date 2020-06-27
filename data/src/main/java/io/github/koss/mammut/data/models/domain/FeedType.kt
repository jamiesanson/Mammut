package io.github.koss.mammut.data.models.domain

import android.os.Parcelable
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
import kotlinx.android.parcel.Parcelize

private const val KEY_HOME_FEED = "home"
private const val KEY_LOCAL_FEED = "local"
private const val KEY_FEDERATED_FEED = "federated"
private const val KEY_ACCOUNT_FEED = "account_toots"
private const val KEY_HASHTAG = "hashtag"

sealed class FeedType(
        val key: String,
        val supportsStreaming: Boolean
) : Parcelable {

    abstract fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>>

    abstract fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)?

    @Parcelize
    object Home : FeedType(KEY_HOME_FEED, false) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? = null

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Timelines(client)::getHome
    }

    @Parcelize
    object Local : FeedType(KEY_LOCAL_FEED, true) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? =
                Streaming(client)::localPublic

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Public(client)::getLocalPublic
    }

    @Parcelize
    object Federated : FeedType(KEY_FEDERATED_FEED, true) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? =
                Streaming(client)::federatedPublic

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Public(client)::getFederatedPublic
    }

    @Parcelize
    data class AccountToots(
            val accountId: Long,
            val withReplies: Boolean,
            val onlyMedia: Boolean
    ) : FeedType("${KEY_ACCOUNT_FEED}_${accountId}_$withReplies", false) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? = null

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> {
            return {
                Accounts(client).getStatuses(
                        accountId = accountId,
                        onlyMedia = onlyMedia,
                        excludeReplies = !withReplies,
                        pinned = false,
                        range = it
                )
            }
        }
    }

    @Parcelize
    data class Hashtag(
        val tag: String
    ): FeedType("${KEY_HASHTAG}_tag", supportsStreaming = true) {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? =
                { handler -> Streaming(client).localHashtag(tag, handler) }

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                { range -> Public(client).getLocalTag(tag, range) }
    }
}
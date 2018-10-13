package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.os.Parcelable
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.Handler
import com.sys1yagi.mastodon4j.api.Pageable
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.Shutdownable
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Public
import com.sys1yagi.mastodon4j.api.method.Streaming
import com.sys1yagi.mastodon4j.api.method.Timelines
import kotlinx.android.parcel.Parcelize


sealed class FeedType: Parcelable {

    abstract fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>>

    abstract fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)?

    @Parcelize
    object Home: FeedType() {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? = null

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Timelines(client)::getHome
    }

    @Parcelize
    object Local: FeedType() {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? =
                Streaming(client)::localPublic

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Public(client)::getLocalPublic
    }

    @Parcelize
    object Federated: FeedType() {
        override fun getStreamingBuilder(client: MastodonClient): ((Handler) -> Shutdownable)? =
                Streaming(client)::federatedPublic

        override fun getRequestBuilder(client: MastodonClient): (Range) -> MastodonRequest<Pageable<Status>> =
                Public(client)::getFederatedPublic
    }
}
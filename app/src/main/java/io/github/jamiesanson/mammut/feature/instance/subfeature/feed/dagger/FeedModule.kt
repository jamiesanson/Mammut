package io.github.jamiesanson.mammut.feature.instance.subfeature.feed.dagger

import android.content.Context
import androidx.room.Room
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Handler
import com.sys1yagi.mastodon4j.api.Shutdownable
import dagger.Module
import dagger.Provides
import io.github.jamiesanson.mammut.data.database.StatusInMemoryDatabase
import io.github.jamiesanson.mammut.data.database.dao.StatusDao
import io.github.jamiesanson.mammut.data.repo.PreferencesRepository
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedType
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.paging.FeedPagingHelper
import javax.inject.Named

@Module(includes = [FeedViewModelModule::class])
class FeedModule(private val feedType: FeedType) {

    @Provides
    @FeedScope
    fun provideStatusDatabase(context: Context): StatusInMemoryDatabase =
            Room.inMemoryDatabaseBuilder(context, StatusInMemoryDatabase::class.java).build()

    @Provides
    @FeedScope
    @Named("in_memory_feed_db")
    fun provideStatusDao(database: StatusInMemoryDatabase): StatusDao =
            database.statusDao()

    @Provides
    @FeedScope
    fun provideStreamingBuilder(mastodonClient: MastodonClient): StreamingBuilder? {
        val builder = feedType.getStreamingBuilder(mastodonClient) ?: return null
        return object : StreamingBuilder {
            override fun startStream(handler: Handler): Shutdownable =
                builder.invoke(handler)

        }
    }

    @Provides
    @FeedScope
    fun provideFeedPagingHelper(
            mastodonClient: MastodonClient,
            pagingCallbacks: FeedPagePreferencesCallbacks,
            streamingBuilder: StreamingBuilder?,
            statusInMemoryDatabase: StatusInMemoryDatabase
    ): FeedPagingHelper =
            FeedPagingHelper(
                    getCallForRange = feedType.getRequestBuilder(mastodonClient),
                    getPreviousPosition = pagingCallbacks.getPage,
                    setPreviousPosition = pagingCallbacks.setPage,
                    streamingBuilder = streamingBuilder,
                    statusDatabase = statusInMemoryDatabase,
                    feedType = feedType)

    @Provides
    @FeedScope
    fun provideType(): FeedType = feedType

    @Provides
    @FeedScope
    fun providePagingPreferencesCallbacks(sharedPreferences: PreferencesRepository): FeedPagePreferencesCallbacks =
            FeedPagePreferencesCallbacks(
                    getPage = {
                        if (sharedPreferences.shouldKeepFeedPlace) {
                            val value= when (feedType) {
                                FeedType.Home -> sharedPreferences.homeFeedLastPageSeen
                                FeedType.Local -> sharedPreferences.localFeedLastPageSeen
                                FeedType.Federated -> sharedPreferences.federatedFeedLastPageSeen
                                is FeedType.AccountToots -> null
                            }

                            if (value != -99999) value else null
                        } else {
                            null
                        }
                    },
                    setPage = {
                        when (feedType) {
                            FeedType.Home -> sharedPreferences.homeFeedLastPageSeen = it
                            FeedType.Local -> sharedPreferences.localFeedLastPageSeen = it
                            FeedType.Federated -> sharedPreferences.federatedFeedLastPageSeen = it
                        }
                    }
            )
}

interface StreamingBuilder {
    fun startStream(handler: Handler): Shutdownable
}

data class FeedPagePreferencesCallbacks(
        val getPage: () -> Int?,
        var setPage: (Int) -> Unit
)
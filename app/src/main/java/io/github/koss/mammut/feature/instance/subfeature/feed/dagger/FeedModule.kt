package io.github.koss.mammut.feature.instance.subfeature.feed.dagger

import android.content.Context
import androidx.room.Room
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Handler
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.Shutdownable
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.base.dagger.scope.FeedScope
import io.github.koss.mammut.data.converters.toLocalModel
import io.github.koss.mammut.data.database.StatusDatabase
import io.github.koss.mammut.data.database.dao.StatusDao
import io.github.koss.mammut.repo.PreferencesRepository
import io.github.koss.mammut.data.extensions.run
import io.github.koss.mammut.data.repository.TootRepository
import io.github.koss.mammut.feature.feedpaging.FeedPager
import io.github.koss.mammut.feature.feedpaging.FeedStateData
import io.github.koss.mammut.feature.feedpaging.initialiseFeedState
import io.github.koss.mammut.feature.instance.subfeature.feed.FeedType
import javax.inject.Named

@Module(includes = [FeedViewModelModule::class])
class FeedModule(private val feedType: FeedType) {

    @Provides
    @FeedScope
    @Named("database_name")
    fun provideDatabaseName(@Named("instance_name") instanceName: String): String =
            "status_${feedType.key}_$instanceName"

    @Provides
    @FeedScope
    fun provideStatusDatabase(
            context: Context,
            @Named("database_name")
            databaseName: String
    ): StatusDatabase =
            when (feedType) {
                is FeedType.AccountToots, FeedType.Federated ->
                    Room.inMemoryDatabaseBuilder(context, StatusDatabase::class.java)
                            .build()
                else -> {
                    Room.databaseBuilder(context, StatusDatabase::class.java, databaseName)
                            .build()
                }
            }

    @Provides
    @FeedScope
    @Named("in_memory_feed_db")
    fun provideStatusDao(database: StatusDatabase): StatusDao =
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
    fun provideFeedPager(mastodonClient: MastodonClient,
                         pagingCallbacks: FeedPagePreferencesCallbacks,
                         streamingBuilder: StreamingBuilder?,
                         statusDatabase: StatusDatabase,
                         feedStateData: FeedStateData): FeedPager =
            FeedPager(
                    getCallForRange = feedType.getRequestBuilder(mastodonClient),
                    getPreviousPosition = pagingCallbacks.getPage,
                    setPreviousPosition = pagingCallbacks.setPage,
                    streamingBuilder = streamingBuilder,
                    statusDatabase = statusDatabase,
                    feedType = feedType,
                    feedStateData = feedStateData
            )

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
                                FeedType.Federated -> null
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
                        }
                    }
            )

    @Provides
    @FeedScope
    fun provideFeedStateData(preferencesRepository: PreferencesRepository,
                             pagingCallbacks: FeedPagePreferencesCallbacks,
                             mastodonClient: MastodonClient,
                             statusDatabase: StatusDatabase): FeedStateData = initialiseFeedState(
            streamingEnabled = feedType.supportsStreaming,
            keepPlaceEnabled = feedType.persistenceEnabled && preferencesRepository.shouldKeepFeedPlace,
            scrolledToTop = pagingCallbacks.getPage() == null || pagingCallbacks.getPage() == 0,
            loadRemotePage = {
                feedType.getRequestBuilder(client = mastodonClient)(Range(limit = 5)).run(retryCount = 3).toOption().orNull()?.part?.map { it.toLocalModel() }
            },
            loadLocalPage = {
                statusDatabase.statusDao().getMostRecent(count = 5, source = feedType.key)
            }
    )

    @Provides
    @FeedScope
    fun provideTootRepository(@Named("instance_access_token") accessToken: String,
                              @Named("instance_name") instanceName: String,
                              @Named("database_name") databaseName: String): TootRepository {
        return TootRepository(instanceName = instanceName, instanceAccessToken = accessToken, databaseName = databaseName)
    }
}

interface StreamingBuilder {
    fun startStream(handler: Handler): Shutdownable
}

data class FeedPagePreferencesCallbacks(
        val getPage: () -> Int?,
        var setPage: (Int) -> Unit
)
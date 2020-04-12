package io.github.koss.mammut.feed.dagger

import android.content.Context
import androidx.room.Room
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.entity.Status
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.data.database.StatusDatabase
import io.github.koss.mammut.data.repository.TootRepository
import io.github.koss.mammut.feed.domain.FeedType
import io.github.koss.mammut.feed.domain.paging.FeedPagingManager
import io.github.koss.mammut.feed.domain.paging.local.DefaultFeedLocalSource
import io.github.koss.mammut.feed.domain.paging.network.DefaultFeedNetworkSource
import io.github.koss.mammut.feed.domain.paging.network.StreamingSupportedFeedNetworkSource
import io.github.koss.mammut.feed.domain.preferences.PreferencesRepository
import io.github.koss.paging.event.PagingRelay
import io.github.koss.paging.local.LocalDataSource
import io.github.koss.paging.network.NetworkDataSource
import kotlinx.coroutines.*
import javax.inject.Named

typealias FeedLocalSource = LocalDataSource<io.github.koss.mammut.data.database.entities.feed.Status>
typealias FeedNetworkSource = NetworkDataSource<Status>

@Module(includes = [FeedViewModelModule::class])
class FeedModule(
        private val feedType: FeedType
) {

    @Provides
    @FeedScope
    fun provideType(): FeedType = feedType

    @Provides
    @FeedScope
    fun provideFeedJob(): Job {
        return Job()
    }

    @Provides
    @FeedScope
    fun provideCoroutineScope(job: Job): CoroutineScope {
        return CoroutineScope(Dispatchers.IO) + job
    }

    @Provides
    @FeedScope
    fun providePagingRelay(): PagingRelay = PagingRelay()

    @Provides
    @FeedScope
    @Named("database_name")
    fun provideDatabaseName(@Named("instance_access_token") accessToken: String): String =
            "status_${feedType.key}_${accessToken.take(4)}"

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
    fun provideNetworkDataSource(
            client: MastodonClient,
            feedType: FeedType
    ): FeedNetworkSource {
        val defaultSource = DefaultFeedNetworkSource(feedType, client)

        if (feedType.supportsStreaming) {
            return StreamingSupportedFeedNetworkSource(defaultSource, feedType, client)
        }

        return defaultSource
    }

    @Provides
    @FeedScope
    fun provideLocalDataSource(
            statusDatabase: StatusDatabase
    ): FeedLocalSource {
        return DefaultFeedLocalSource(statusDao = statusDatabase.statusDao())
    }

    @Provides
    @FeedScope
    fun provideFeedPagingManager(
            @FeedScope scope: CoroutineScope,
            relay: PagingRelay,
            localDataSource: FeedLocalSource,
            networkDataSource: FeedNetworkSource): FeedPagingManager {
        return FeedPagingManager(scope, relay, localDataSource, networkDataSource)
    }

    @Provides
    @FeedScope
    fun provideTootRepository(@Named("instance_access_token") accessToken: String,
                              @Named("instance_name") instanceName: String,
                              @Named("database_name") databaseName: String): TootRepository {
        return TootRepository(instanceName = instanceName, instanceAccessToken = accessToken, databaseName = databaseName)
    }

    @Provides
    @FeedScope
    fun providePreferencesRepository(context: Context): PreferencesRepository = PreferencesRepository(context)
}
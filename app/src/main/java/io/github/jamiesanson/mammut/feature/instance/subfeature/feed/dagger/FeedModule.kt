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
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedPagingManager
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedType
import javax.inject.Named

@Module(includes = [FeedViewModelModule::class])
class FeedModule(private val feedType: FeedType) {

    @Provides
    @FeedScope
    fun provideFeedPagingManager(mastodonClient: MastodonClient): FeedPagingManager
        = FeedPagingManager(feedType.getRequestBuilder(mastodonClient))

    @Provides
    @FeedScope
    @Named("in_memory_feed_db")
    fun provideStatusDao(context: Context): StatusDao =
            Room.inMemoryDatabaseBuilder(context, StatusInMemoryDatabase::class.java).build().statusDao()

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
    fun provideType(): FeedType = feedType
}

interface StreamingBuilder {
    fun startStream(handler: Handler): Shutdownable
}
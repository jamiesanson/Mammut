package io.github.koss.mammut.toot.dagger

import com.sys1yagi.mastodon4j.MastodonClient
import dagger.Module
import dagger.Provides
import io.github.koss.mammut.data.repository.InstanceDetailRepository
import io.github.koss.mammut.toot.repo.StatusRepository

@Module(includes = [ComposeTootViewModelModule::class])
class ComposeTootModule {

    @Provides
    @ComposeTootScope
    fun provideStatusRepository(client: MastodonClient, instanceDetailRepository: InstanceDetailRepository): StatusRepository {
        return StatusRepository(client, instanceDetailRepository)
    }
}
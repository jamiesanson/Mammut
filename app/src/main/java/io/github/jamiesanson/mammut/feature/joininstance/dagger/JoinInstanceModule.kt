package io.github.jamiesanson.mammut.feature.joininstance.dagger

import dagger.Module
import dagger.Provides
import io.github.jamiesanson.mammut.feature.joininstance.JoinInstanceReducer
import io.github.jamiesanson.mammut.feature.joininstance.JoinInstanceState
import io.github.koss.randux.createStore
import io.github.koss.randux.utils.Store
import javax.inject.Named

@Module(includes = [ JoinInstanceViewModelModule::class ])
object JoinInstanceModule {

    const val JOIN_INSTANCE_STORE = "join_instance_store"

    @Provides
    @JvmStatic
    @JoinInstanceScope
    @Named(JOIN_INSTANCE_STORE)
    fun provideStore(): Store = createStore(
            reducer = JoinInstanceReducer(),
            preloadedState = JoinInstanceState()
    )
}
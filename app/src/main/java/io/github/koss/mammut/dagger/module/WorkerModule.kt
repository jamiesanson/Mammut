package io.github.koss.mammut.dagger.module

import androidx.work.WorkerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.github.koss.mammut.base.dagger.scope.ApplicationScope
import io.github.koss.mammut.dagger.worker.MammutWorkerFactory
import io.github.koss.mammut.dagger.worker.WorkerKey
import io.github.koss.mammut.data.extensions.ClientBuilder
import io.github.koss.mammut.data.work.SingleWorkerFactory
import io.github.koss.mammut.data.work.tootinteraction.TootInteractionWorker

@Module(includes = [ WorkerFactoryModule::class ])
class WorkerModule {

    @Provides
    @IntoMap
    @WorkerKey(TootInteractionWorker::class)
    fun provideTootInteractionWorkerFactory(clientBuilder: ClientBuilder): SingleWorkerFactory =
            TootInteractionWorker.factory(clientBuilder)
}

@Module
abstract class WorkerFactoryModule {

    @Binds
    @ApplicationScope
    abstract fun bindWorkerFactory(factory: MammutWorkerFactory): WorkerFactory
}
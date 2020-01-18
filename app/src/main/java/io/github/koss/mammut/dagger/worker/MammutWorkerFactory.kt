package io.github.koss.mammut.dagger.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import io.github.koss.mammut.data.work.SingleWorkerFactory
import javax.inject.Inject
import javax.inject.Provider

/**
 * WorkerFactory for Mammut, used for homebrew assisted injection of workers.
 */
class MammutWorkerFactory @Inject constructor(
        private val creators: Map<Class<out ListenableWorker>, @JvmSuppressWildcards Provider<SingleWorkerFactory>>
) : WorkerFactory() {

    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        creators.asSequence()
                .firstOrNull { (clazz, _) -> clazz.name == workerClassName }?.value?.get()?.invoke(appContext, workerParameters)
}
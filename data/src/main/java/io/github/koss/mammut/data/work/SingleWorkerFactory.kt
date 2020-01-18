package io.github.koss.mammut.data.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters

typealias SingleWorkerFactory = (appContext: Context, workerParams: WorkerParameters) -> ListenableWorker
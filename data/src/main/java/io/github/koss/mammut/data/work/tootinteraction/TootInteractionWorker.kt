package io.github.koss.mammut.data.work.tootinteraction

import android.content.Context
import androidx.room.Room
import androidx.work.*
import com.sys1yagi.mastodon4j.api.method.Statuses
import io.github.koss.mammut.data.converters.toEntity
import io.github.koss.mammut.data.database.StatusDatabase
import io.github.koss.mammut.data.extensions.ClientBuilder
import io.github.koss.mammut.data.extensions.run
import io.github.koss.mammut.data.work.SingleWorkerFactory

/**
 * Worked used for interacting with a toot. This included things like boosting and retooting.
 */
class TootInteractionWorker(
        private val clientBuilder: ClientBuilder,
        appContext: Context,
        workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams) {

    /**
     * Class defining actions that can be taken by this worker
     */
    enum class Action {
        BOOST,
        UNBOOST,
        RETOOT,
        UNRETOOT
    }

    override suspend fun doWork(): Result {
        val statusId = inputData.getLong(INPUT_STATUS_ID, -1L)
        val action = Action.values()[inputData.getInt(INPUT_ACTION, -1)]

        if (statusId == -1L) return Result.failure()

        val instanceName = inputData.getString(INPUT_INSTANCE_NAME) ?: return Result.failure()
        val accessToken = inputData.getString(INPUT_ACCESS_TOKEN) ?: return Result.failure()

        // Get a [Statuses] instance
        val client = clientBuilder.getInstanceBuilder(instanceName).accessToken(accessToken).build()
        val statuses = Statuses(client)

        val request = when (action) {
            Action.BOOST -> statuses.postFavourite(statusId)
            Action.UNBOOST -> statuses.postUnfavourite(statusId)
            Action.RETOOT -> statuses.postReblog(statusId)
            Action.UNRETOOT -> statuses.postUnreblog(statusId)
        }

        val updatedStatus = request.run(retryCount = 3).toOption().orNull() ?: return Result.failure()

        val databaseName = inputData.getString(INPUT_DATABASE_NAME) ?: return Result.failure()

        Room.databaseBuilder(applicationContext, StatusDatabase::class.java, databaseName)
                .build()
                .statusDao()
                .insertStatus(updatedStatus.toEntity())

        return Result.success()
    }

    companion object {
        const val INPUT_STATUS_ID = "status_id"
        const val INPUT_ACTION = "action"
        const val INPUT_INSTANCE_NAME = "instance_name"
        const val INPUT_ACCESS_TOKEN = "access_token"
        const val INPUT_DATABASE_NAME = "database_name"

        @JvmStatic
        fun factory(clientBuilder: ClientBuilder): SingleWorkerFactory = { context, params ->
            TootInteractionWorker(clientBuilder, context, params)
        }

        @JvmStatic
        fun workRequestBuilder(statusId: Long, action: Action, instanceName: String, accessToken: String, databaseName: String): OneTimeWorkRequest.Builder {
            return OneTimeWorkRequestBuilder<TootInteractionWorker>()
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setInputData(Data.Builder()
                            .putLong(INPUT_STATUS_ID, statusId)
                            .putInt(INPUT_ACTION, action.ordinal)
                            .putString(INPUT_INSTANCE_NAME, instanceName)
                            .putString(INPUT_ACCESS_TOKEN, accessToken)
                            .putString(INPUT_DATABASE_NAME, databaseName)
                            .build()
                    )
        }
    }
}
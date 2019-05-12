package io.github.koss.mammut.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.work.*
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.data.extensions.getUniqueWorkTag
import io.github.koss.mammut.data.extensions.workTag
import io.github.koss.mammut.data.models.StatusState
import io.github.koss.mammut.data.work.WorkConstants
import io.github.koss.mammut.data.work.tootinteraction.TootInteractionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TootRepository(
        private val instanceName: String,
        private val instanceAccessToken: String,
        private val databaseName: String
) {

    /**
     * Function for toggling the boost state of a status. If there is a pending boost, it will cancel this
     * and not do anything.
     */
    suspend fun toggleBoostForStatus(status: Status): Operation.State.SUCCESS = withContext(Dispatchers.Default) {
        val state = loadStatusState(status)

        if (state.isBoostPending) {
            // Cancel boost
            WorkManager.getInstance().cancelUniqueWork(status.getUniqueWorkTag(TOGGLE_BOOST_ACTION)).await()
        } else {
            // Toggle boost
            val toggleAction = if (status.isFavourited) TootInteractionWorker.Action.UNBOOST else TootInteractionWorker.Action.BOOST

            val workRequest = newTootInteractionRequest(status, toggleAction)
                    .addTag(toggleAction.toString())
                    .build()

            WorkManager.getInstance().enqueueUniqueWork(
                    status.getUniqueWorkTag(TOGGLE_BOOST_ACTION),
                    ExistingWorkPolicy.KEEP,
                    workRequest
            ).await()
        }
    }

    /**
     * Function for toggling the retoot state of a status. If there is a pending retoot, it will cancel this
     * and not do anything.
     */
    suspend fun toggleRetootForStatus(status: Status): Operation.State.SUCCESS = withContext(Dispatchers.Default) {
        val state = loadStatusState(status)

        if (state.isRetootPending) {
            // Cancel retoot
            WorkManager.getInstance().cancelUniqueWork(status.getUniqueWorkTag(TOGGLE_RETOOT_ACTION)).await()
        } else {
            // Toggle retoot
            val toggleAction = if (status.isReblogged) TootInteractionWorker.Action.UNRETOOT else TootInteractionWorker.Action.RETOOT

            val workRequest = newTootInteractionRequest(status, toggleAction)
                    .addTag(toggleAction.toString())
                    .build()

            WorkManager.getInstance().enqueueUniqueWork(
                    status.getUniqueWorkTag(TOGGLE_RETOOT_ACTION),
                    ExistingWorkPolicy.KEEP,
                    workRequest
            ).await()
        }
    }

    /**
     * Function for loading auxiliary state of a status. Currently this function loads whether or not there's a boost
     * or retoot pending.
     */
    suspend fun loadStatusState(status: Status): StatusState = withContext(Dispatchers.Default) {
        val workInfo = WorkManager.getInstance().getWorkInfosByTag(status.workTag).get()

        return@withContext StatusState(
                isBoostPending = workInfo
                        .filter { !it.state.isFinished }
                        .map { it.tags }
                        .any { it.contains(WorkConstants.TootInteraction.TAG_BOOST) || it.contains(WorkConstants.TootInteraction.TAG_UNBOOST) },
                isRetootPending = workInfo
                        .filter { !it.state.isFinished }
                        .map { it.tags }
                        .any { it.contains(WorkConstants.TootInteraction.TAG_RETOOT) || it.contains(WorkConstants.TootInteraction.TAG_UNRETOOT) }
        )
    }

    /**
     * Function for loading auxiliary state of a status. Currently this function loads whether or not there's a boost
     * or retoot pending.
     */
    fun loadStatusStateLive(status: Status): LiveData<Pair<Status, StatusState>> {
        val workInfoLive = WorkManager.getInstance().getWorkInfosByTagLiveData(status.workTag)

        return Transformations.map(workInfoLive) { workInfo ->
            status to StatusState(
                    isBoostPending = workInfo
                            .filter { !it.state.isFinished }
                            .map { it.tags }
                            .any { it.contains(WorkConstants.TootInteraction.TAG_BOOST) || it.contains(WorkConstants.TootInteraction.TAG_UNBOOST) },
                    isRetootPending = workInfo
                            .filter { !it.state.isFinished }
                            .map { it.tags }
                            .any { it.contains(WorkConstants.TootInteraction.TAG_RETOOT) || it.contains(WorkConstants.TootInteraction.TAG_UNRETOOT) }
            )
        }
    }


    private fun newTootInteractionRequest(status: Status, action: TootInteractionWorker.Action): OneTimeWorkRequest.Builder =
            TootInteractionWorker.workRequestBuilder(
                    statusId = status.id,
                    action = action,
                    accessToken = instanceAccessToken,
                    instanceName = instanceName,
                    databaseName = databaseName
            )

    companion object {
        private const val TOGGLE_BOOST_ACTION = "toggle_boost"
        private const val TOGGLE_RETOOT_ACTION = "toggle_retoot"
    }
}
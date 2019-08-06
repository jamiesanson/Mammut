package io.github.koss.mammut.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.work.*
import io.github.koss.mammut.data.extensions.getUniqueWorkTag
import io.github.koss.mammut.data.extensions.workTag
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.data.models.StatusState
import io.github.koss.mammut.data.work.WorkConstants
import io.github.koss.mammut.data.work.containsSuccessfulBoost
import io.github.koss.mammut.data.work.containsSuccessfulUnboost
import io.github.koss.mammut.data.work.hasPendingBoostFor
import io.github.koss.mammut.data.work.tootinteraction.TootInteractionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TootRepository(
        private val instanceName: String,
        private val instanceAccessToken: String,
        private val databaseName: String
) {

    /**
     * Function for hiding all the underlying offline hackery which goes in to maintaining the state
     * of the status.
     */
    fun getStatusStateLive(status: Status): LiveData<Pair<Status, StatusState>> =
            Transformations.map(WorkManager.getInstance().getWorkInfosByTagLiveData(status.workTag)) {
                status.copy(
                        isFavourited = when {
                            it.containsSuccessfulBoost(status) -> true
                            it.containsSuccessfulUnboost(status) -> false
                            else -> status.isFavourited
                        }
                ) to it.toStatusState(status)
            }

    /**
     * Function for toggling the boost state of a status. If there is a pending boost, it will cancel this
     * and not do anything.
     */
    suspend fun toggleBoostForStatus(status: Status): Operation.State.SUCCESS = withContext(Dispatchers.Default) {
        val hasPendingBoost = WorkManager.getInstance()
                .getWorkInfosByTag(status.workTag)
                .await()
                .hasPendingBoostFor(status)

        if (hasPendingBoost) {
            // Cancel boost
            WorkManager.getInstance().cancelUniqueWork(status.getUniqueWorkTag(TOGGLE_BOOST_ACTION)).await()
        } else {
            // Toggle boost
            val toggleAction = if (status.isFavourited) TootInteractionWorker.Action.UNBOOST else TootInteractionWorker.Action.BOOST

            val workRequest = newTootInteractionRequest(status, toggleAction)
                    .addTag(toggleAction.workTag)
                    .addTag(status.workTag)
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
                    .addTag(toggleAction.workTag)
                    .addTag(status.workTag)
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
    private suspend fun loadStatusState(status: Status): StatusState = withContext(Dispatchers.Default) {
        val workInfo = WorkManager.getInstance().getWorkInfosByTag(status.workTag).get()

        return@withContext workInfo.toStatusState(status)
    }

    private fun List<WorkInfo>.toStatusState(status: Status) = StatusState(
            isBoostPending = this
                    .filter { !it.state.isFinished }
                    .map { it.tags }
                    .filter { it.contains(status.workTag) }
                    .any { it.contains(WorkConstants.TootInteraction.TAG_BOOST) || it.contains(WorkConstants.TootInteraction.TAG_UNBOOST) },
            isRetootPending = this
                    .filter { !it.state.isFinished }
                    .map { it.tags }
                    .filter { it.contains(status.workTag) }
                    .any { it.contains(WorkConstants.TootInteraction.TAG_RETOOT) || it.contains(WorkConstants.TootInteraction.TAG_UNRETOOT) })

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
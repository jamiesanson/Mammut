package io.github.koss.mammut.feed.ui.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.koss.mammut.data.repository.TootRepository
import io.github.koss.mammut.feed.presentation.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
class StatusViewModel @Inject constructor(
    private val tootRepository: TootRepository
): ViewModel() {

    val statusOverrides = ConflatedBroadcastChannel(value = StatusOverrides())

    // Transient state used by the View
    var isContentVisible = false

    lateinit var currentStatus: StatusModel

    private val job: Job = Job()

    fun onBoostClicked() {
        viewModelScope.launch {
            tootRepository.toggleBoostForStatus(currentStatus.status)
        }
    }

    fun onRetootClicked() {
        viewModelScope.launch {
            tootRepository.toggleRetootForStatus(currentStatus.status)
        }
    }

    fun onNewModel(model: StatusModel) {
        job.cancelChildren()
        currentStatus = model

        // Process submission time
        viewModelScope.launch(job) {
            val submissionTime = ZonedDateTime.parse(model.createdAt)

            while (true) {
                val timeSinceSubmission = Duration.between(submissionTime, ZonedDateTime.now())
                val newOverrides = statusOverrides.value.copy(submissionTime = timeSinceSubmission.toElapsedTime())
                statusOverrides.send(newOverrides)
                delay(1000)
            }
        }

        // Process boost/retoot state
        viewModelScope.launch(job) {
            tootRepository.getStatusStateLive(model.status)
                .asFlow()
                .filterNotNull()
                .collect { (status, state) ->
                    currentStatus = currentStatus.copy(status = status)

                    val newOverrides = statusOverrides.value.copy(
                        isBoosted = status.isFavourited.takeUnless { state.isBoostPending },
                        isRetooted = status.isReblogged.takeUnless { state.isRetootPending }
                    )

                    statusOverrides.send(newOverrides)
                }
        }
    }

    private fun Duration.toElapsedTime(): String =
        when {
            this > Duration.of(7, ChronoUnit.DAYS) -> "${toDays() / 7}w"
            this > Duration.of(1, ChronoUnit.DAYS) -> "${toDays()}d"
            this > Duration.of(1, ChronoUnit.HOURS) -> "${toHours()}h"
            this > Duration.of(1, ChronoUnit.MINUTES) -> "${toMinutes()}m"
            this > Duration.of(1, ChronoUnit.SECONDS) -> "${toMillis() / 1000}s"
            else -> "Now"
        }

    @ExperimentalCoroutinesApi
    private fun <T> LiveData<T>.asFlow() = channelFlow {
        trySend(value)
        val observer = Observer<T> { t -> trySend(t) }
        withContext(Dispatchers.Main) {
            observeForever(observer)
        }
        awaitClose {
            removeObserver(observer)
        }
    }

}

data class StatusOverrides(
    val isRetooted: Boolean? = null,
    val isBoosted: Boolean? = null,
    val submissionTime: String = ""
)
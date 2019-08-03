package io.github.koss.mammut.feature.instance.subfeature.feed

import android.content.Context
import android.util.Log
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import arrow.data.invalid
import com.sys1yagi.mastodon4j.api.entity.Attachment
import io.github.koss.mammut.data.converters.toModel
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.data.models.StatusState
import io.github.koss.mammut.data.repository.TootRepository
import io.github.koss.mammut.extension.postSafely
import io.github.koss.mammut.feature.base.Event
import io.github.koss.mammut.toot.emoji.EmojiRenderer
import kotlinx.coroutines.*
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for handling presentation logic of a Toot.
 */
class TootViewModel @Inject constructor(
        private val context: Context,
        private val tootRepository: TootRepository
) : ViewModel(), CoroutineScope by GlobalScope {

    val currentStatus: Status
        get() = statusStateLiveData.value?.first
                ?: throw UninitializedPropertyAccessException("No value for status found")

    private val statusLiveData: LiveData<Status> = MutableLiveData()

    private val statusStateLiveData = Transformations
            .switchMap(statusLiveData) {
                tootRepository.getStatusStateLive(it)
            }

    val statusViewState: LiveData<TootViewState> = Transformations
            .switchMap(statusStateLiveData) { (status, state) ->
                processViewState(status, state)
            }

    val timeSince: LiveData<String> = MutableLiveData()

    // transient state used by the view
    var isContentVisible = false

    private var countJob: Job? = null

    fun bind(status: Status) {
        if (status == statusLiveData.value) return

        statusLiveData.postSafely(status)
        processSubmissionTime()
    }

    fun onBoostClicked() {
        launch {
            tootRepository.toggleBoostForStatus(statusStateLiveData.value!!.first)
        }
    }

    fun onRetootClicked() {
        launch {
            tootRepository.toggleRetootForStatus(statusStateLiveData.value!!.first)
        }
    }

    public override fun onCleared() {
        super.onCleared()
        countJob?.cancel()
        (timeSince as MutableLiveData).value = null
    }

    private fun processViewState(status: Status, state: StatusState): LiveData<TootViewState> {
        val liveDataState = MutableLiveData<TootViewState>()

        val name = (if (status.account?.displayName?.isEmpty() == true) status.account!!.acct else status.account?.displayName)
                ?: ""
        val username = "@${status.account?.acct ?: status.account?.userName}"
        val content = HtmlCompat.fromHtml(status.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()

        liveDataState.postValue(TootViewState(name, username, content, status.mediaAttachments,
                avatar = status.account?.avatar!!,
                spoilerText = status.spoilerText,
                isSensitive = status.isSensitive,
                isRetooted = status.isReblogged.takeUnless { state.isRetootPending },
                isBoosted = status.isFavourited.takeUnless { state.isBoostPending },
                retootCount = status.reblogsCount,
                boostCount = status.favouritesCount))

        launch {
            // Post the HTML rendered content first such that it displays earlier.
            val renderedContent = async {
                EmojiRenderer.render(context, content, emojis = status.emojis?.map { it.toModel() }
                        ?: emptyList())
            }

            liveDataState.postValue(TootViewState(
                    name,
                    username,
                    renderedContent.await(),
                    status.mediaAttachments,
                    avatar = status.account?.avatar!!,
                    spoilerText = status.spoilerText,
                    isSensitive = status.isSensitive,
                    isRetooted = status.isReblogged.takeUnless { state.isRetootPending },
                    isBoosted = status.isFavourited.takeUnless { state.isBoostPending },
                    retootCount = status.reblogsCount,
                    boostCount = status.favouritesCount))
        }

        return liveDataState
    }

    private fun processSubmissionTime() {
        val status = statusLiveData.value ?: return
        val submissionTime = ZonedDateTime.parse(status.createdAt)

        // Configure counting
        countJob?.cancel()
        countJob = GlobalScope.launch {
            while (true) {
                withContext(Dispatchers.Main) {
                    val timeSinceSubmission = Duration.between(submissionTime, ZonedDateTime.now())
                    timeSince.postSafely(timeSinceSubmission.toElapsedTime())
                }
                delay(1000)
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
}

data class TootViewState(
        val name: String,
        val username: String,
        val content: CharSequence,
        val displayAttachments: List<Attachment<*>>,
        val isRetooted: Boolean?,
        val isBoosted: Boolean?,
        val avatar: String,
        val spoilerText: String,
        val isSensitive: Boolean,
        val boostCount: Int,
        val retootCount: Int
)
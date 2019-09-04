package io.github.koss.mammut.feed.ui.status

import android.content.Context
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sys1yagi.mastodon4j.api.entity.Attachment
import io.github.koss.emoji.EmojiRenderer
import io.github.koss.mammut.data.converters.toNetworkModel
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.data.models.StatusState
import io.github.koss.mammut.data.repository.TootRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class StatusViewModel @Inject constructor(
    private val context: Context,
    private val tootRepository: TootRepository
): ViewModel() {

    @Suppress("EXPERIMENTAL_API_USAGE")
    val viewState: ConflatedBroadcastChannel<StatusViewState> = ConflatedBroadcastChannel()

    @Suppress("EXPERIMENTAL_API_USAGE")
    val timeSince: ConflatedBroadcastChannel<String> = ConflatedBroadcastChannel()

    // Transient state used by the View
    var isContentVisible = false

    var currentStatus: Status? by Delegates.observable<Status?>(initialValue = null) { _, old, new ->
        if (old != new) {
            new?.let(::onNewStatus)
        }
    }

    fun submitStatus(status: Status) {
        currentStatus = status
    }

    fun onBoostClicked() {
        viewModelScope.launch {
            tootRepository.toggleBoostForStatus(currentStatus!!)
        }
    }

    fun onRetootClicked() {
        viewModelScope.launch {
            tootRepository.toggleRetootForStatus(currentStatus!!)
        }
    }

    private fun onNewStatus(status: Status) {
        val firstState = processMinimal(status)
        @Suppress("EXPERIMENTAL_API_USAGE")
        viewState.offer(firstState)

        // Begin async work
        viewModelScope.launch {
            @Suppress("EXPERIMENTAL_API_USAGE")
            tootRepository.getStatusStateLive(status)
                .asFlow()
                .filterNotNull()
                .map { (status, state) -> processStatus(status, state) }
                .collect {
                    viewState.offer(it)
                }

            // Increment time since
            processSubmissionTime(status)
        }
    }

    private fun processMinimal(status: Status): StatusViewState {
        val name = (if (status.account?.displayName?.isEmpty() == true) status.account!!.acct else status.account?.displayName)
            ?: ""
        val username = "@${status.account?.acct ?: status.account?.userName}"
        val content = HtmlCompat.fromHtml(status.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()

        return StatusViewState(name, username, content, status.mediaAttachments,
            avatar = status.account?.avatar!!,
            spoilerText = status.spoilerText,
            isSensitive = status.isSensitive,
            isRetooted = false,
            isBoosted = false,
            retootCount = status.reblogsCount,
            boostCount = status.favouritesCount)
    }

    private suspend fun processStatus(status: Status, state: StatusState): StatusViewState = coroutineScope {
        val name = (if (status.account?.displayName?.isEmpty() == true) status.account!!.acct else status.account?.displayName)
            ?: ""
        val username = "@${status.account?.acct ?: status.account?.userName}"

        val content = HtmlCompat.fromHtml(status.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()


        // Post the HTML rendered content first such that it displays earlier.
        val renderedContent = EmojiRenderer.render(context, content, emojis = status.emojis?.map { it.toNetworkModel() }
                ?: emptyList())

        return@coroutineScope StatusViewState(
            name,
            username,
            renderedContent,
            status.mediaAttachments,
            avatar = status.account?.avatar!!,
            spoilerText = status.spoilerText,
            isSensitive = status.isSensitive,
            isRetooted = status.isReblogged.takeUnless { state.isRetootPending },
            isBoosted = status.isFavourited.takeUnless { state.isBoostPending },
            retootCount = status.reblogsCount,
            boostCount = status.favouritesCount
        )
    }

    private suspend fun processSubmissionTime(status: Status) = coroutineScope {
        val submissionTime = ZonedDateTime.parse(status.createdAt)

        launch {
            while (true) {
                val timeSinceSubmission = Duration.between(submissionTime, ZonedDateTime.now())
                timeSince.offer(timeSinceSubmission.toElapsedTime())
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

    @ExperimentalCoroutinesApi
    private fun <T> LiveData<T>.asFlow() = channelFlow {
        offer(value)
        val observer = Observer<T> { t -> offer(t) }
        observeForever(observer)
        awaitClose {
            removeObserver(observer)
        }
    }

}

data class StatusViewState(
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
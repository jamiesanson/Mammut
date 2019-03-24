package io.github.koss.mammut.feature.instance.subfeature.feed

import android.content.Context
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sys1yagi.mastodon4j.api.entity.Attachment
import io.github.koss.mammut.data.converters.toModel
import io.github.koss.mammut.data.database.entities.feed.Status
import io.github.koss.mammut.data.models.Emoji
import io.github.koss.mammut.data.repository.InstanceDetailRepository
import io.github.koss.mammut.extension.postSafely
import io.github.koss.mammut.toot.emoji.EmojiRenderer
import kotlinx.coroutines.*
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * ViewModel for handling presentation logic of a Toot.
 */
class TootViewModel @Inject constructor(
        private val context: Context,
        private val instanceDetailRepository: InstanceDetailRepository,
        @Named("instance_name")
        private val instanceName: String
): ViewModel(), CoroutineScope by GlobalScope {

    val statusViewState: LiveData<TootViewState> = MutableLiveData()

    val timeSince: LiveData<String> = MutableLiveData()

    var currentStatus: Status? = null

    private var emojis: Deferred<List<Emoji>> = async { instanceDetailRepository.loadEmojisForInstance(instanceName) }

    private var countJob = Job()

    fun bind(status: Status) {
        if (status.id == currentStatus?.id) return

        currentStatus = status
        processViewState()
        processSubmissionTime()
    }

    public override fun onCleared() {
        super.onCleared()
        countJob.cancel()
        (statusViewState as MutableLiveData).value = null
        (timeSince as MutableLiveData).value = null
    }

    private fun processViewState() {
        val status = currentStatus ?: return
        val name = (if (status.account?.displayName?.isEmpty() == true) status.account!!.acct else status.account?.displayName) ?: ""
        val username = "@${status.account?.acct ?: status.account?.userName}"

        launch(Dispatchers.IO) {
            val content = HtmlCompat.fromHtml(status.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
            statusViewState.postSafely(TootViewState(name, username, content, status.mediaAttachments.firstOrNull()))

            // Post the HTML rendered content first such that it displays earlier.
            val renderedContent = EmojiRenderer.render(context, content, emojis = emojis.await().map { it.toModel() })
            statusViewState.postSafely(TootViewState(name, username, renderedContent, status.mediaAttachments.firstOrNull()))
        }
    }

    private fun processSubmissionTime() {
        val status = currentStatus ?: return
        val submissionTime = ZonedDateTime.parse(status.createdAt)

        // Configure counting
        countJob.cancel()
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
        val displayAttachment: Attachment<*>?
)
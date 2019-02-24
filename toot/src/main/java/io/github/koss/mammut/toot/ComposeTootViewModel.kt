package io.github.koss.mammut.toot

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sys1yagi.mastodon4j.api.entity.Emoji
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.toot.emoji.EmojiRenderer
import io.github.koss.mammut.toot.model.SubmissionState
import io.github.koss.mammut.toot.model.TootModel
import io.github.koss.mammut.toot.repo.StatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.Bold
import org.jetbrains.anko.Underline
import javax.inject.Inject

class ComposeTootViewModel @Inject constructor(
        private val context: Context,
        private val statusRepository: StatusRepository
) : ViewModel(), CoroutineScope by GlobalScope {

    val model: LiveData<TootModel> = MutableLiveData()

    val renderedStatus: LiveData<SpannableStringBuilder> = Transformations.switchMap(model, ::renderStatus)

    val submissionState: LiveData<SubmissionState> = MutableLiveData()

    val availableEmojis: LiveData<List<Emoji>> = MutableLiveData()

    val hasBeenModified: Boolean
        get() = model.value != null && model.value?.copy(inReplyToId = null) != emptyModel

    private val emptyModel: TootModel = TootModel(
            status = "",
            inReplyToId = null,
            mediaIds = null,
            sensitive = false,
            spoilerText = null
    )

    private var textHeight: Int = 64

    init {
        // Load emojis
        launch {
            val emojis = statusRepository.loadVisibleEmojis()
            (availableEmojis as MutableLiveData).postValue(emojis)
        }
    }

    /**
     * Function for initialising the ViewModel. Useful when resetting the model, and
     * also allows supplying of an `inReplyToId`
     */
    fun initialise(inReplyToId: Long?, textHeight: Int, force: Boolean = false) {
        // Initialise
        this.textHeight = textHeight

        if (!hasBeenModified || force) {
            updateModel {
                if (inReplyToId != null) {
                    emptyModel.copy(inReplyToId = inReplyToId)
                } else {
                    // TODO - Retrieve from local cache if it exists
                    emptyModel.copy(inReplyToId = inReplyToId)
                }
            }
        }
    }

    fun onStatusChanged(status: String) {
        if (status == model.value?.status) return

        updateModel { it?.copy(status = formatStatus(it.status, status)) }
    }

    /**
     * Updates status with emoji text when clicked. Only updates the status text if
     * the resulting text is shorter than [MAX_TOOT_LENGTH]
     */
    fun onEmojiAdded(emoji: Emoji, index: Int) {
        updateModel {
            val status = it?.status ?: ""
            val emojiText = ":${emoji.shortcode}:"

            when {
                ("$status$emojiText").length <= MAX_TOOT_LENGTH -> {
                    it?.copy(status = StringBuilder(status).insert(index, emojiText).toString())
                }
                else -> it
            }
        }
    }

    fun onVisibilityChanged(visibility: Status.Visibility) {
        updateModel { it?.copy(visibility = visibility) }
    }

    fun deleteTootContents() {
        // Reset everything but the replyToId
        initialise(model.value?.inReplyToId, textHeight, force = true)
    }

    /**
     * Function for submitting the toot.
     */
    fun onSendTootClicked() {
        model.value?.let {
            // Validate
            if (it.status.isEmpty()) throw IllegalStateException("Status can't be empty")

            // Begin Loading
            (submissionState as MutableLiveData).value = SubmissionState(
                    isSubmitting = true
            )

            // Submit Toot
            launch(Dispatchers.IO) {
                submissionState.postValue(statusRepository.post(it))
            }
        }
    }

    /**
     * Utility function for updating the underlying model
     */
    private fun updateModel(updater: (TootModel?) -> TootModel?) {
        (model as MutableLiveData).value = updater(model.value)
    }

    /**
     * Function for formatting the status text when it's modified. Used primarily for deletion of
     * emoji when a colon is found to be deleted.
     */
    private fun formatStatus(oldStatus: String, newStatus: String): String {
        // If there's a deletion of a colon, we should check if we can delete an entire emoji
        if (newStatus.filter { it == ':' }.length < oldStatus.filter { it == ':' }.length) {
            // Iterate along until the first colon which is different.
            oldStatus.forEachIndexed { index, c ->
                // if we encounter the first change and it happens to not be an emoji, finish
                if (newStatus.getOrNull(index) != c) {
                    when (c) {
                        ':' -> {
                            // Search backwards for an emoji
                            val potentialEmoji = oldStatus.substring(0 until index)
                                    .takeLastWhile { it != ':' }

                            // Check to see if the deleted text was actually an emoji
                            val emojiDeleted = availableEmojis.value?.any {
                                it.shortcode == potentialEmoji
                            } ?: false

                            // If emoji deleted, remove it from the string
                            if (emojiDeleted) {
                                val emojiStartIndex = oldStatus
                                        .substring(0 until index)
                                        .indexOfLast { it == ':' }

                                return oldStatus.substring(0 until emojiStartIndex) + oldStatus.substring(index + 1 until oldStatus.length)
                            }
                        }
                        else -> return newStatus
                    }
                }
            }
        }

        // if we haven't returned by this point, return the new status
        return newStatus
    }

    /**
     * Function for rendering the status off the main thread - works with switchMap to allow
     * asynchronous rendering.
     */
    private fun renderStatus(model: TootModel): LiveData<SpannableStringBuilder> {
        val liveData = MutableLiveData<SpannableStringBuilder>()
        val status = model.status

        launch {
            liveData.postValue(EmojiRenderer.render(context, status, availableEmojis.value ?: emptyList(), textHeight))
        }

        return liveData
    }
}
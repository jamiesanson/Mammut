package io.github.koss.mammut.toot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.method.Statuses
import io.github.koss.mammut.toot.model.SubmissionState
import io.github.koss.mammut.toot.model.TootModel
import io.github.koss.mammut.toot.repo.StatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class ComposeTootViewModel @Inject constructor(
        private val mastodonClient: MastodonClient
): ViewModel(), CoroutineScope by GlobalScope {

    val model: LiveData<TootModel> = MutableLiveData()

    val submissionState: LiveData<SubmissionState> = MutableLiveData()

    val hasBeenModified: Boolean
        get() = model.value != null && model.value?.copy(inReplyToId = null) != emptyModel

    val statusRepo: StatusRepository by lazy { StatusRepository(mastodonClient) }

    private val emptyModel: TootModel = TootModel(
            status = "",
            inReplyToId = null,
            mediaIds = null,
            sensitive = false,
            spoilerText = null
    )

    /**
     * Function for initialising the ViewModel. Useful when resetting the model, and
     * also allows supplying of an `inReplyToId`
     */
    fun initialise(inReplyToId: Long?, force: Boolean = false) {
        // Initialise
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
        updateModel { it?.copy(status = status) }
    }

    fun deleteTootContents() {
        // Reset everything but the replyToId
        initialise(model.value?.inReplyToId, force = true)
    }

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
                submissionState.postValue(statusRepo.post(it))
            }
        }
    }

    private fun updateModel(updater: (TootModel?) -> TootModel?) {
        (model as MutableLiveData).value = updater(model.value)
    }
}
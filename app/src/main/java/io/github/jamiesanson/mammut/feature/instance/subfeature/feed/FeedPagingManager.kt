package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.Pageable
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.extension.run
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.coroutineContext

class FeedPagingManager(
        private val getCallForRange: (Range) -> MastodonRequest<Pageable<Status>>
) {

    private lateinit var getLatestId: suspend () -> Long?
    private lateinit var getEarliestId: suspend () -> Long?

    private var isInitialised = false

    /**
     * The results to be exposed by the repository
     */
    val feedResults: LiveData<List<Status>> = MutableLiveData()

    /**
     * LiveData where error messages are emitted
     */
    var errors: LiveData<String> = MutableLiveData()

    /**
     * MUST BE CALLED BEFORE PAGING BEGINS - NOT A GOOD PATTERN BUT WHATEVER
     */
    suspend fun initialise(startingRange: Range, getLatestId: suspend () -> Long?, getEarliestId: suspend () -> Long?) {
        this.getLatestId = getLatestId
        this.getEarliestId = getEarliestId

        // Post the initial results
        if (startingRange.sinceId == Range().sinceId && startingRange.maxId == Range().maxId) {
            val results = loadForRange(startingRange)
            isInitialised = true
            feedResults.postSafely(results)
        } else {
            isInitialised = true
        }
    }

    /**
     * Called when a status ID is binded - loading around it.
     *
     * The logic's a bit weird on this one. If the ID is the max ID, we're at the top of the chronological timeline
     */
    fun loadAroundId(id: Long) {
        if (isInitialised) {
            launch {
                loadAroundIdSuspending(id)
            }
        }
    }

    /**
     * Called when a status ID is binded - loading around it.
     *
     * The logic's a bit weird on this one. If the ID is the max ID, we're at the top of the chronological timeline
     */
    suspend fun loadAroundIdSuspending(id: Long) {
        if (isInitialised) {
            when (id) {
                getLatestId() -> {
                    // Load start
                    val results = loadForRange(Range(sinceId = id))
                    feedResults.postSafely(results)
                }
                getEarliestId() -> {
                    // Load end
                    val results = loadForRange(Range(maxId = id))
                    feedResults.postSafely(results)
                }
                else -> {
                    // Ignore this
                }
            }
        }
    }

    /**
     * Function for loading results for a given range. Returns an empty list
     */
    private suspend fun loadForRange(range: Range): List<Status> {
        val maxLengthRange = Range(maxId = range.maxId, sinceId = range.sinceId, limit = 40)
        val results = getCallForRange(maxLengthRange).run(retryCount = 3)
        return when (results) {
            is Either.Left -> {
                errors.postSafely(results.a)
                emptyList()
            }
            is Either.Right -> {
                if (results.b.part.isNotEmpty()) {
                    results.b.part
                } else {
                    emptyList()
                }
            }
        }
    }
}
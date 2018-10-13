package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.Pageable
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.jamiesanson.mammut.extension.postSafely
import io.github.jamiesanson.mammut.extension.run
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.launch

class FeedPagingManager(
        private val getCallForRange: (Range) -> MastodonRequest<Pageable<Status>>
) {

    private lateinit var currentRange: Range
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
            // If the starting range isn't empty, we're loading some items from the DB so leave as is
            currentRange = startingRange
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
                when (id) {
                    getLatestId() -> {
                        // Load start
                        launch {
                            val results = loadForRange(Range(sinceId = id))
                            Log.d("FeedPaging", "Loaded start: ${results.map { it.id }}")
                            feedResults.postSafely(results)
                        }
                    }
                    getEarliestId() -> {
                        // Load end
                        launch {
                            val results = loadForRange(Range(maxId = id))
                            Log.d("FeedPaging", "Loaded end: ${results.map { it.id }}")
                            feedResults.postSafely(results)
                        }
                    }
                    else -> {
                        // Ignore this
                    }
                }
            }
        }
    }

    /**
     * Function for loading results for a given range. Returns an empty list
     */
    private suspend fun loadForRange(range: Range): List<Status> {
        val results = getCallForRange(range).run(retryCount = 3)
        return when (results) {
            is Either.Left -> {
                errors.postSafely(results.a)
                emptyList()
            }
            is Either.Right -> {
                if (results.b.part.isNotEmpty()) {
                    currentRange = results.b.toRange()
                    results.b.part
                } else {
                    emptyList()
                }
            }
        }
    }
}
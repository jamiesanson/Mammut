package io.github.jamiesanson.mammut.feature.feedpaging

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import arrow.core.Either
import com.sys1yagi.mastodon4j.MastodonRequest
import com.sys1yagi.mastodon4j.api.Pageable
import com.sys1yagi.mastodon4j.api.Range
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.extension.awaitFirst
import io.github.jamiesanson.mammut.extension.run
import kotlinx.coroutines.*
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

/**
 * Boundary callback for handling paging
 */
class FeedPagingBoundaryCallback(
        ioExecutor: Executor,
        private val getCallForRange: (Range) -> MastodonRequest<Pageable<com.sys1yagi.mastodon4j.api.entity.Status>>,
        private val handleStatuses: suspend (List<com.sys1yagi.mastodon4j.api.entity.Status>, isInFront: Boolean) -> Unit,
        private val feedState: LiveData<FeedState>
) : PagedList.BoundaryCallback<Status>(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO // TODO - Add error handler specific to the helper

    val helper = PagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            executeStatusRequest(getCallForRange(Range()), it)
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: Status) {
        super.onItemAtFrontLoaded(itemAtFront)
        // If timeline broken, or we're streaming, we shouldn't go ahead with this.
        launch {
            // The following only applies if in upwards paging state
            if (feedState.awaitFirst() !is FeedState.PagingUpwards) return@launch

            withContext(Dispatchers.Main) {
                helper.runIfNotRunning(PagingRequestHelper.RequestType.BEFORE) {
                    executeStatusRequest(getCallForRange(Range(minId = itemAtFront.id)), it, isLoadingInfront = true)
                }
            }
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: Status) {
        super.onItemAtEndLoaded(itemAtEnd)
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            executeStatusRequest(getCallForRange(Range(maxId = itemAtEnd.id)), it)
        }
    }

    private fun executeStatusRequest(request: MastodonRequest<Pageable<com.sys1yagi.mastodon4j.api.entity.Status>>, callback: PagingRequestHelper.Request.Callback, isLoadingInfront: Boolean = false) {
        launch {
            val result = request.run(retryCount = 3)

            when (result) {
                is Either.Left -> {
                    callback.recordFailure(Exception(result.a.description))
                }
                is Either.Right -> {
                    handleStatuses(result.b.part, isLoadingInfront)

                    callback.recordSuccess()
                }
            }
        }
    }

}
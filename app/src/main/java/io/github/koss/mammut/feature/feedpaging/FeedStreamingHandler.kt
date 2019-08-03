package io.github.koss.mammut.feature.feedpaging

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.sys1yagi.mastodon4j.api.Handler
import com.sys1yagi.mastodon4j.api.Shutdownable
import com.sys1yagi.mastodon4j.api.entity.Notification
import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.feature.base.Event
import io.github.koss.mammut.feature.instance.subfeature.feed.dagger.StreamingBuilder
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class FeedStreamingHandler(
        private val onItemDeleted: suspend (Long) -> Unit,
        private val handleStatuses: suspend (List<Status>) -> Unit,
        private val streamingBuilder: StreamingBuilder?,
        private val feedState: LiveData<FeedState>
): Handler, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private var streamShutdownable: Shutdownable? = null

    private var bufferedItemDisposable: Disposable? = null

    private val itemStreamedPublishSubject = PublishSubject.create<Status>()

    private val bufferedItemFlowable: Flowable<Event<List<Status>>> = Flowable
            .fromPublisher(itemStreamedPublishSubject.toFlowable(BackpressureStrategy.BUFFER))
            .buffer(20, TimeUnit.MILLISECONDS)
            .map { Event(it) }

    val itemStreamed: LiveData<Event<List<Status>>> = LiveDataReactiveStreams
            .fromPublisher(bufferedItemFlowable)

    init {
        feedState.observeForever(::onFeedStateChanged)
    }

    private fun onFeedStateChanged(feedState: FeedState) {
        when (feedState) {
            FeedState.StreamingFromTop -> {
                initialise()
            }
        }
    }

    private fun initialise() {
        launch {
            streamShutdownable = streamingBuilder?.startStream(this@FeedStreamingHandler)
        }

        // Begin listening to buffered results
        bufferedItemDisposable = bufferedItemFlowable
                .subscribeOn(Schedulers.io())
                .subscribe{
                    launch {
                        handleStatuses(it.peekContent())
                    }
                }
    }

    fun tearDown() {
        feedState.removeObserver(::onFeedStateChanged)

        launch {
            streamShutdownable?.shutdown()
            streamShutdownable = null
            bufferedItemDisposable?.dispose()
        }
    }

    override fun onDelete(id: Long) {
        launch {
            onItemDeleted(id)
        }
    }

    override fun onNotification(notification: Notification) {
        // no-op for now
    }

    override fun onStatus(status: Status) {
        itemStreamedPublishSubject.onNext(status)
    }

}
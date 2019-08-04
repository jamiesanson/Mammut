package io.github.koss.paging.event

import io.github.koss.paging.event.DataEndReached
import io.github.koss.paging.event.DataStartReached
import io.github.koss.paging.event.PagingEvent
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel

class PagingRelay: BroadcastChannel<PagingEvent> by ConflatedBroadcastChannel() {

    fun endOfDataDisplayed() = offer(DataStartReached)

    fun startOfDataDisplayed() = offer(DataEndReached)
}
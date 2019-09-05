@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.github.koss.paging.event

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel

class PagingRelay: BroadcastChannel<PagingEvent> by ConflatedBroadcastChannel() {

    fun endOfDataDisplayed() = offer(DataEndReached)

    fun startOfDataDisplayed() = offer(DataStartReached)

    fun onItemStreamed() = offer(ItemStreamed)
}
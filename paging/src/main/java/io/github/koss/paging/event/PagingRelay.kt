@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.github.koss.paging.event

import kotlinx.coroutines.flow.MutableSharedFlow

class PagingRelay: MutableSharedFlow<PagingEvent> by MutableSharedFlow() {

    fun endOfDataDisplayed() = tryEmit(DataEndReached)

    fun startOfDataDisplayed() = tryEmit(DataStartReached)

    fun onItemStreamed() = tryEmit(ItemStreamed)
}
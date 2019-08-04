package io.github.koss.paging.event

sealed class PagingEvent

object NoDataDisplayed: PagingEvent()

object DataStartReached: PagingEvent()

object DataEndReached: PagingEvent()


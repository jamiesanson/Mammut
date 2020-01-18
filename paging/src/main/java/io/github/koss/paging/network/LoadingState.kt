package io.github.koss.paging.network

sealed class LoadingState

object LoadingAtEnd: LoadingState()

object LoadingAtFront: LoadingState()

object LoadingAll: LoadingState()

object NotLoading: LoadingState()
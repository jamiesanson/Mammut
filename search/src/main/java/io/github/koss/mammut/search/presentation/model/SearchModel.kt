package io.github.koss.mammut.search.presentation.model

sealed class SearchModel

data class Account(
    val accountId: Long,
    val acct: String,
    val displayName: String,
    val avatar: String
): SearchModel()

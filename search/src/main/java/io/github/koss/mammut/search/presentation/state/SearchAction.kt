package io.github.koss.mammut.search.presentation.state

import com.sys1yagi.mastodon4j.api.entity.Results

sealed class SearchAction

data class OnLoadStart(
    val query: String
): SearchAction()

data class OnResults(
    val query: String,
    val results: Results
): SearchAction()
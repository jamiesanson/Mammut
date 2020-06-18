package io.github.koss.mammut.search.presentation.state

import com.sys1yagi.mastodon4j.api.entity.Results

sealed class SearchAction

object OnLoadStart: SearchAction()

data class OnResults(
    val results: Results
): SearchAction()
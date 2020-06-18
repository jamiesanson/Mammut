package io.github.koss.mammut.search.presentation.state

import io.github.koss.mammut.search.presentation.model.SearchModel

sealed class SearchState

object NoResults: SearchState()

object Loading: SearchState()

data class Loaded(
    val results: List<SearchModel>
): SearchState()
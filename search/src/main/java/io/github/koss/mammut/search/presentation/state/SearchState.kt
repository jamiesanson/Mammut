package io.github.koss.mammut.search.presentation.state

import io.github.koss.mammut.search.presentation.model.SearchModel

sealed class SearchState(val query: String)

class NoResults(
    query: String
): SearchState(query)

class Loading(
    query: String
): SearchState(query)

class Loaded(
    query: String,
    val results: List<SearchModel>
): SearchState(query)
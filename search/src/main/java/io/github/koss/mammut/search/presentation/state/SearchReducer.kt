package io.github.koss.mammut.search.presentation.state

import com.sys1yagi.mastodon4j.api.entity.Results
import io.github.koss.mammut.search.presentation.model.Account
import io.github.koss.mammut.search.presentation.model.SearchModel
import io.github.koss.randux.utils.Action
import io.github.koss.randux.utils.Reducer
import io.github.koss.randux.utils.State

class SearchReducer : Reducer {
    override fun invoke(currentState: State?, incomingAction: Action): State? {
        return when (incomingAction) {
            is OnLoadStart -> Loading
            is OnResults -> if (incomingAction.results.accounts.isEmpty()) {
                NoResults
            } else {
                Loaded(incomingAction.results.toSearchModels())
            }
            else -> currentState
        }
    }
}

private fun Results.toSearchModels(): List<SearchModel> {
    return accounts.map {
        Account(
            accountId = it.id,
            displayName = it.displayName,
            acct = it.acct,
            avatar = it.avatar
        )
    }
}
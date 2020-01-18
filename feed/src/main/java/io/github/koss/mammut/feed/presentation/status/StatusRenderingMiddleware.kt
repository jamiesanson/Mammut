package io.github.koss.mammut.feed.presentation.status

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.text.HtmlCompat
import io.github.koss.emoji.EmojiRenderer
import io.github.koss.mammut.data.converters.toNetworkModel
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.feed.presentation.model.StatusModel
import io.github.koss.mammut.feed.presentation.state.OnItemsLoaded
import io.github.koss.mammut.feed.presentation.state.OnItemsRendered
import io.github.koss.randux.utils.Dispatch
import io.github.koss.randux.utils.Middleware
import io.github.koss.randux.utils.MiddlewareAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Middleware for intercepting [OnItemsLoaded] actions and processing items. The overall result
 * is an asynchronous mapping from [Status] to [StatusModel]
 */
class StatusRenderingMiddleware(
    parentScope: CoroutineScope,
    private val applicationContext: Context
): Middleware, CoroutineScope by parentScope {

    @SuppressLint("UseSparseArrays")
    private val renderedCache = HashMap<Long, StatusModel>()

    /**
     * Middleware function
     */
    override fun invoke(api: MiddlewareAPI): (next: Dispatch) -> Dispatch = { next -> { action ->
        when (action) {
            is OnItemsLoaded -> launch(Dispatchers.IO) {
                val items = flowOf(*action.items.toTypedArray())
                    .map { it.toFeedModel() }
                    .toList()

                withContext(Dispatchers.Main) {
                    next(OnItemsRendered(items))
                }
            }
            else -> next(action)
        }
    }}

    private suspend fun Status.toFeedModel(): StatusModel {
        // If we have a cached model, return it
        renderedCache[id]?.let {
            return it
        }

        val name = (if (account?.displayName?.isEmpty() == true) account!!.acct else account?.displayName)
            ?: ""
        val username = "@${account?.acct ?: account?.userName}"
        val renderedUsername = EmojiRenderer.render(applicationContext, username, emojis = emojis?.map { it.toNetworkModel() }
                ?: emptyList())

        val content = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()

        val renderedContent = EmojiRenderer.render(applicationContext, content, emojis = emojis?.map { it.toNetworkModel() }
            ?: emptyList())

        val model = StatusModel(
            id = id,
            name = name,
            username = username,
            renderedUsername = renderedUsername,
            renderedContent = renderedContent,
            createdAt = createdAt,
            displayAttachments = mediaAttachments,
            avatar = account?.avatar!!,
            spoilerText = spoilerText,
            isSensitive = isSensitive,
            isRetooted = isReblogged,
            isBoosted = isFavourited,
            retootCount = reblogsCount,
            boostCount = favouritesCount,
            status = this
        )

        renderedCache[id] = model

        return model
    }
}
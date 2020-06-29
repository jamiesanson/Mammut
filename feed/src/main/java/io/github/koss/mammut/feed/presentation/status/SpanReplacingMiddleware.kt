package io.github.koss.mammut.feed.presentation.status

import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import androidx.core.text.getSpans
import io.github.koss.mammut.data.models.Status
import io.github.koss.mammut.data.models.Tag
import io.github.koss.mammut.feed.presentation.event.Navigation
import io.github.koss.mammut.feed.presentation.state.OnItemsRendered
import io.github.koss.randux.utils.Dispatch
import io.github.koss.randux.utils.Middleware
import io.github.koss.randux.utils.MiddlewareAPI

class SpanReplacingMiddleware(
    private val onNavigationEvent: (Navigation) -> Unit
): Middleware {
    override fun invoke(api: MiddlewareAPI): (next: Dispatch) -> Dispatch = { next -> { action ->
        when (action) {
            // Catch the ItemsRendered action and replace URL spans with clickable spans which
            // fire off navigation events
            is OnItemsRendered -> {
                if (action.spansReplaced) {
                    next(action)
                } else {
                    next(action.replaceSpans())
                }
            }
            else -> next(action)
        }
    }}

    private fun OnItemsRendered.replaceSpans(): OnItemsRendered {
        renderedItems.forEachIndexed { index, statusModel ->
            (statusModel.renderedContent as? SpannableStringBuilder)?.apply {
                replaceMentions(items[index])
                replaceTags()
            }
        }

        return copy(spansReplaced = true)
    }

    private fun SpannableStringBuilder.replaceMentions(status: Status) {
        replaceUrlSpans(findMatchingModel = { url -> status.mentions.find { it.mentionUrl == url } }) { mention ->
            onNavigationEvent(Navigation.Profile(userId = mention.mentionId.toString()))
        }
    }

    private fun SpannableStringBuilder.replaceTags() {
        fun matchUrl(url: String): Tag? {
            val uri = Uri.parse(url)

            if (uri.pathSegments.firstOrNull() == "tags") {
                return uri.pathSegments
                    .drop(1)
                    .firstOrNull()?.let { Tag(tagName = it) }
            }

            return null
        }

        replaceUrlSpans(findMatchingModel = ::matchUrl) { tag ->
            onNavigationEvent(Navigation.Tag(tagName = tag.tagName))
        }
    }

    private fun <T> SpannableStringBuilder.replaceUrlSpans(findMatchingModel: (String) -> T?, onSpanClicked: (T) -> Unit) {
        getSpans<URLSpan>(0, length).forEach { span ->
            val model = findMatchingModel(span.url)
            if (model != null) {
                val (start, end) = getSpanStart(span) to getSpanEnd(span)
                val newSpan = object: ClickableSpan() {
                    override fun onClick(widget: View) {
                        onSpanClicked(model)
                    }
                }

                removeSpan(span)
                setSpan(newSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
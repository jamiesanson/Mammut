package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.sys1yagi.mastodon4j.api.entity.Attachment
import com.sys1yagi.mastodon4j.api.entity.GifvAttachment
import com.sys1yagi.mastodon4j.api.entity.PhotoAttachment
import com.sys1yagi.mastodon4j.api.entity.VideoAttachment
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.GlideApp
import io.github.jamiesanson.mammut.data.database.entities.feed.Status
import io.github.jamiesanson.mammut.extension.inflate
import kotlinx.android.synthetic.main.view_holder_feed_item.view.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.jetbrains.anko.image
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.floor

class TootViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.view_holder_feed_item)) {

    private var countJob = Job()

    private var currentStatus: Status? = null

    fun bind(status: Status, callbacks: TootCallbacks, requestManager: RequestManager) {
        if (status.id == currentStatus?.id) return

        currentStatus = status

        val submissionTime = ZonedDateTime.parse(status.createdAt)

        with(itemView) {
            displayNameTextView.text = if (status.account?.displayName?.isEmpty() == true) status.account.acct else status.account?.displayName
            usernameTextView.text = "@${status.account?.userName}"
            contentTextView.text = HtmlCompat.fromHtml(status.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()

            countJob.cancel()
            countJob = launch {
                while (true) {
                    withContext(UI) {
                        val timeSinceSubmission = Duration.between(submissionTime, ZonedDateTime.now())
                        timeTextView.text = timeSinceSubmission.toElapsedTime()
                    }
                    delay(1, TimeUnit.SECONDS)
                }
            }

            status.account?.let { account ->
                profileImageView.onClick {
                    callbacks.onProfileClicked(account)
                }
            }

            // Resolve colors
            val typedValue = TypedValue()
            val theme = itemView.context.theme ?: return
            theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
            @ColorInt val color = typedValue.data

            requestManager
                    .load(status.account?.avatar)
                    .thumbnail(
                            requestManager
                                    .load(ColorDrawable(color))
                                    .apply(RequestOptions.circleCropTransform())
                    )
                    .transition(withCrossFade())
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileImageView)

            status.mediaAttachments.firstOrNull()?.let {
                requestManager
                        .clear(tootImageView)

                val aspect = getThumbnailSpec(it)

                if (tootImageCardView.isInvisible) tootImageCardView.isVisible = true

                fun onImageViewLaidOut(view: View) {
                    view.updateLayoutParams imageView@ {
                        height = floor(view.width / aspect).toInt()
                    }
                    view.doOnLayout { _ ->
                        loadAttachment(it, requestManager)
                    }
                }

                if (tootImageCardView.width > 0) {
                    // The imageView is already laid out, therefore we don't need to wait for the next pass
                    onImageViewLaidOut(tootImageCardView)
                } else {
                    tootImageCardView.doOnLayout(::onImageViewLaidOut)
                }

                tootImageCardView.onClick { _ ->
                    callbacks.onPhotoClicked(tootImageView, it.url)
                }
            } ?: run {
                tootImageCardView.updateLayoutParams {
                    height = 0

                    doOnLayout {
                        tootImageView.image = null
                        tootImageCardView.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    private fun loadAttachment(attachment: Attachment<*>, requestManager: RequestManager) {
        // Resolve colors
        val typedValue = TypedValue()
        val theme = itemView.context.theme ?: return
        theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
        @ColorInt val color = typedValue.data

        // Load attachment
        requestManager
                .load(attachment.url)
                .thumbnail(
                        requestManager
                                .load(attachment.previewUrl)
                                .thumbnail(
                                        requestManager
                                                .load(ColorDrawable(color))
                                )
                                .transition(withCrossFade())
                )
                .transition(withCrossFade())
                .into(itemView.tootImageView)
    }

    /**
     * Function for inspecting an attachment for metadata and retrieving an approximate
     * width and height. Will assume 4:3 width:height ratio if nothing found
     */
    private fun getThumbnailSpec(attachment: Attachment<*>): Float {
        val bestGuess = 400F / 300F
        return when (attachment) {
            is PhotoAttachment -> attachment.metadata?.original?.run {
                when {
                    aspect != 0F -> aspect
                    width != 0 && height != 0 -> (width / height).toFloat()
                    else -> bestGuess
                }
            } ?: bestGuess
            is VideoAttachment -> attachment.metadata?.original?.run {
                if (width != 0 && height != 0) (width / height).toFloat() else bestGuess
            } ?: bestGuess
            is GifvAttachment -> attachment.metadata?.original?.run {
                if (width != 0 && height != 0) (width / height).toFloat() else bestGuess
            } ?: bestGuess
            else -> throw IllegalArgumentException("Unknown attachment type")
        }
    }

    private fun Duration.toElapsedTime(): String =
            when {
                this > Duration.of(7, ChronoUnit.DAYS) -> "${toDays().rem(7)} weeks ago"
                this > Duration.of(1, ChronoUnit.DAYS) -> "${toDays()} days ago"
                this > Duration.of(1, ChronoUnit.HOURS) -> "${toHours()} hours ago"
                this > Duration.of(1, ChronoUnit.MINUTES) -> "${toMinutes()} mins ago"
                this > Duration.of(1, ChronoUnit.SECONDS) -> "${toMillis() / 1000} secs ago"
                else -> "Just now"
            }


    fun clear() {
        with(itemView) {
            displayNameTextView.text = null
            usernameTextView.text = null
            contentTextView.text = null
            timeTextView.text = null
            profileImageView.setOnClickListener(null)
            GlideApp.with(itemView)
                    .clear(profileImageView)
            GlideApp.with(itemView)
                    .clear(tootImageView)
            tootImageView.visibility = View.GONE
        }
        countJob.cancel()
        currentStatus = null
    }
}
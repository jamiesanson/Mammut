package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
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
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class TootViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.view_holder_feed_item)) {

    private var countJob = Job()

    fun bind(status: Status, callbacks: TootCallbacks) {
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

            GlideApp.with(itemView)
                    .load(status.account?.avatar)
                    .thumbnail(
                            GlideApp.with(itemView)
                                    .load(ColorDrawable(color))
                                    .apply(RequestOptions.circleCropTransform())
                    )
                    .transition(withCrossFade())
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileImageView)

            status.mediaAttachments.firstOrNull()?.let {
                tootImageCardView.visibility = View.VISIBLE

                GlideApp.with(itemView)
                        .clear(tootImageView)

                // Force a layout to ensure the imageview resizes properly
                tootImageView.layout(0,0,0,0)

                // Load attachment
                GlideApp.with(itemView)
                        .load(it.attachmentUrl)
                        .thumbnail(
                                GlideApp.with(itemView)
                                        .load(it.previewUrl)
                                        .fitCenter()
                        )
                        .transition(withCrossFade())
                        .fitCenter()
                        .into(tootImageView)
            } ?: run {
                tootImageCardView.visibility = View.GONE
            }
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
            tootImageCardView.visibility = View.GONE
        }
        countJob.cancel()
    }
}
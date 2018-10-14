package io.github.jamiesanson.mammut.feature.instance.subfeature.feed

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
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
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class TootViewHolder(parent: ViewGroup): RecyclerView.ViewHolder(parent.inflate(R.layout.view_holder_feed_item)) {

    private var countJob = Job()

    fun bind(status: Status) {
        val submissionTime = ZonedDateTime.parse(status.createdAt)

        with (itemView) {
            displayNameTextView.text = status.account?.displayName
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

            GlideApp.with(itemView)
                    .load(status.account?.avatar)
                    .thumbnail(
                            GlideApp.with(itemView)
                                    .load(ColorDrawable(
                                            ResourcesCompat.getColor(itemView.resources, R.color.standardPrimaryLightColor, itemView.context.theme))
                                    )
                                    .apply(RequestOptions.circleCropTransform())
                    )
                    .transition(withCrossFade())
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileImageView)
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
        with (itemView) {
            displayNameTextView.text = null
            usernameTextView.text = null
            contentTextView.text = null
            timeTextView.text = null
            GlideApp.with(itemView)
                    .clear(profileImageView)
        }
        countJob.cancel()
    }
}
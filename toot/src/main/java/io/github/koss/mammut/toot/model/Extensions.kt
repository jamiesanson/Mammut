package io.github.koss.mammut.toot.model

import com.sys1yagi.mastodon4j.api.entity.Status
import io.github.koss.mammut.toot.R

val (Status.Visibility).iconRes get() = when (this) {
    Status.Visibility.Public -> R.drawable.ic_public_black_24dp
    Status.Visibility.Unlisted -> R.drawable.ic_lock_open_black_24dp
    Status.Visibility.Private -> R.drawable.ic_lock_outline_black_24dp
    Status.Visibility.Direct -> R.drawable.ic_mail_black_24dp
}
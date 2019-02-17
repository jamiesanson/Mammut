package io.github.koss.mammut.toot

import androidx.lifecycle.ViewModel
import com.sys1yagi.mastodon4j.MastodonClient
import javax.inject.Inject

class ComposeTootViewModel @Inject constructor(
        private val mastodonClient: MastodonClient
): ViewModel() {

}
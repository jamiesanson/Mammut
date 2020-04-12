package io.github.koss.mammut.feed.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.domain.FeedType
import kotlinx.android.synthetic.main.feed_fragment.*

// This is due to a limitation of the navigation library, allowing us to use default parcelable args
class HomeFeedFragment: FeedFragment() {
    override var feedType: FeedType = FeedType.Home
}
class LocalFeedFragment: FeedFragment(){
    override var feedType: FeedType = FeedType.Local
}
class FederatedFeedFragment: FeedFragment(){
    override var feedType: FeedType = FeedType.Federated
}

open class FeedFragment: Fragment(R.layout.feed_fragment) {

    protected open lateinit var feedType: FeedType

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar.isVisible = true
        Log.d("FeedFragment", "Feed for type: $feedType")
    }
}
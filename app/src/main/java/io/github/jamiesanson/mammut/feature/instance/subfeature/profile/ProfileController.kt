package io.github.jamiesanson.mammut.feature.instance.subfeature.profile

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.GlideApp
import io.github.jamiesanson.mammut.component.retention.retained
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.data.models.Account
import io.github.jamiesanson.mammut.extension.observe
import io.github.jamiesanson.mammut.feature.instance.InstanceActivity
import io.github.jamiesanson.mammut.feature.instance.subfeature.navigation.BaseController
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger.ProfileModule
import io.github.jamiesanson.mammut.feature.instance.subfeature.profile.dagger.ProfileScope
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.fragment_profile.*
import org.jetbrains.anko.bundleOf
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import androidx.annotation.ColorInt
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.transition.TransitionManager
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.bumptech.glide.load.MultiTransformation
import com.google.android.material.button.MaterialButton
import io.github.jamiesanson.mammut.feature.instance.subfeature.FullScreenPhotoHandler
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedController
import io.github.jamiesanson.mammut.feature.instance.subfeature.feed.FeedType
import org.jetbrains.anko.sdk25.coroutines.onClick


@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class ProfileController(args: Bundle): BaseController(args), FullScreenPhotoHandler {

    private lateinit var viewModel: ProfileViewModel

    @Inject
    @ProfileScope
    lateinit var viewModelFactory: MammutViewModelFactory

    private val key: String
        get() = "Profile_${args.getParcelable<Account>(ARG_ACCOUNT)}_${profileOpenCount.incrementAndGet()}"

    private val profileModule: ProfileModule by retained(key = { key }) {
        val account: Account? = args.getParcelable(ARG_ACCOUNT)
        if (account == null && !args.getBoolean(ARG_IS_ME)) throw IllegalArgumentException("No Account or isMe flag set")
        ProfileModule(account)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
            inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        (context as InstanceActivity)
                .component
                .plus(profileModule)
                .inject(this)

        viewModel = ViewModelProviders.of(context, viewModelFactory).get(key, ProfileViewModel::class.java)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        setupToolbar()
        viewModel.accountLiveData.observe(this, ::bindAccount)
        viewModel.followStateLiveData.observe(this, ::bindFollowButton)
    }

    override fun displayFullScreenPhoto(imageView: ImageView, photoUrl: String) {
        (parentController as? FullScreenPhotoHandler)?.displayFullScreenPhoto(imageView, photoUrl)
    }

    private fun setupToolbar() {
        val context = view?.context ?: return

        // Resolve colors
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true)
        @ColorInt val color = typedValue.data

        // Set icon
        toolbar.navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_black_24dp)?.apply {
            setTint(color)
            setTintMode(PorterDuff.Mode.SRC_IN)
        }

        // Setup back button
        toolbar.setNavigationOnClickListener {
            router.popCurrentController()
        }
    }

    private fun setupViewPager(account: Account) {
        val pagerAdapter = object: RouterPagerAdapter(this) {

            override fun configureRouter(router: Router, position: Int) {
                if (!router.hasRootController()) {
                    val controller = when (position) {
                        0 -> FeedController.newInstance(FeedType.AccountToots(
                                accountId = account.accountId,
                                withReplies = false
                        ))
                        1 -> FeedController.newInstance(FeedType.AccountToots(
                                accountId = account.accountId,
                                withReplies = true
                        ))
                        else -> return
                    }

                    router.setRoot(RouterTransaction.with(controller))
                }
            }

            override fun getCount(): Int = 3

            override fun getPageTitle(position: Int): CharSequence? = when (position) {
                0 -> "Toots"
                1 -> "w/ replies"
                2 -> "Media"
                else -> throw IndexOutOfBoundsException("Unexpected index")
            }
        }

        tabLayout.setupWithViewPager(pager)
        pager.adapter = pagerAdapter
    }

    private fun bindAccount(account: Account) {
        // Resolve colors
        val typedValue = TypedValue()
        val theme = view?.context?.theme ?: return
        theme.resolveAttribute(R.attr.colorPrimaryTransparency, typedValue, true)
        @ColorInt val color = typedValue.data

        // Profile image
        GlideApp.with(profileImageView)
                .load(account.avatar)
                .thumbnail(
                        GlideApp.with(profileImageView)
                                .load(ColorDrawable(color))
                                .apply(RequestOptions.circleCropTransform())
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(profileImageView)

        // Header image
        GlideApp.with(coverPhotoImageView)
                .load(account.header)
                .thumbnail(
                        GlideApp.with(coverPhotoImageView)
                                .load(ColorDrawable(color))
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.bitmapTransform(MultiTransformation(BlurTransformation(25), ColorFilterTransformation(color))))
                .into(coverPhotoImageView)

        usernameTextView.text = "@${account.acct}"
        displayNameTextView.text = if (account.displayName.isEmpty()) account.acct else account.displayName
        descriptionTextView.text = HtmlCompat.fromHtml(account.note, HtmlCompat.FROM_HTML_MODE_COMPACT)

        tootCountTextView.text = account.statusesCount.toString()
        followingCountTextView.text = account.followingCount.toString()
        followerCountTextView.text = account.followersCount.toString()

        setupViewPager(account)
    }

    private fun bindFollowButton(followState: FollowState) {
        fun MaterialButton.showLoading() {
            TransitionManager.beginDelayedTransition(view as ViewGroup)
            visibility = View.VISIBLE
            isEnabled = false
        }

        fun MaterialButton.hideLoading() {
            TransitionManager.beginDelayedTransition(view as ViewGroup)
            visibility = View.VISIBLE
            isEnabled = true
        }

        when (followState) {
            is FollowState.Following -> {
                if (followState.loadingUnfollow) {
                    followButton.showLoading()
                } else {
                    followButton.hideLoading()
                }
                followButton.text = view?.context?.getString(R.string.unfollow)
            }
            is FollowState.NotFollowing -> {
                if (followState.loadingFollow) {
                    followButton.showLoading()
                } else {
                    followButton.hideLoading()
                }
                followButton.text = view?.context?.getString(R.string.follow)
            }
            FollowState.IsMe -> {
                followButton.visibility = View.GONE
            }
        }

        followButton.onClick {
            viewModel.requestFollowToggle(followState)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(account: Account? = null, isMe: Boolean = true): ProfileController =
                ProfileController(bundleOf(
                            ARG_ACCOUNT to account,
                            ARG_IS_ME to isMe
                    ))

        private val profileOpenCount = AtomicInteger()

        private const val ARG_ACCOUNT = "account"
        private const val ARG_IS_ME = "is_me"
    }
}
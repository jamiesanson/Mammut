package io.github.koss.mammut.feature.profile

import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.text.HtmlCompat
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.transition.TransitionManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.R
import io.github.koss.mammut.base.anko.colorAttr
import io.github.koss.mammut.base.dagger.scope.ProfileScope
import io.github.koss.mammut.base.dagger.viewmodel.MammutViewModelFactory
import io.github.koss.mammut.base.photoviewer.FullScreenPhotoDelegate
import io.github.koss.mammut.base.photoviewer.FullScreenPhotoViewer
import io.github.koss.mammut.base.util.*
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.data.models.NetworkState
import io.github.koss.mammut.data.models.domain.FeedType
import io.github.koss.mammut.databinding.ProfileFragmentBinding
import io.github.koss.mammut.feature.profile.dagger.ProfileComponent
import io.github.koss.mammut.feature.profile.dagger.ProfileModule
import io.github.koss.mammut.feature.profile.domain.FollowState
import io.github.koss.mammut.feature.profile.presentation.ProfileViewModel
import io.github.koss.mammut.feed.ui.FeedFragment
import io.github.koss.mammut.feed.ui.FeedFragmentArgs
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class ProfileFragment: Fragment(R.layout.profile_fragment), FullScreenPhotoViewer by FullScreenPhotoDelegate() {

    private val binding by viewLifecycleLazy { ProfileFragmentBinding.bind(requireView()) }

    private lateinit var viewModel: ProfileViewModel

    @Inject
    @ProfileScope
    lateinit var viewModelFactory: MammutViewModelFactory

    private val args: ProfileFragmentArgs by navArgs()

    private val key: String
        get() = "Profile_${args.accountId}_${profileOpenCount.incrementAndGet()}"

    private val module by lazy {
        if (args.accountId == -1L && !args.isMe) throw IllegalArgumentException("No Account or isMe flag set")
        ProfileModule(if (args.accountId == -1L) null else args.accountId)
    }

    private val component by retained(key = ::key) {
        findSubcomponentFactory()
                .buildSubcomponent<ProfileModule, ProfileComponent>(module)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(key, ProfileViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        viewModel.accountLiveData.observe(this, ::bindAccount)
        viewModel.followStateLiveData.observe(this, ::bindFollowButton)
        viewModel.networkState.observe(this, ::bindNetworkState)

        setPhotoTargetViewBinding {
            binding.fullScreenPhotoLayout to binding.fullScreenGestureImageView
        }
    }

    private fun setupToolbar() {
        binding.appBarLayout.doOnApplyWindowInsets { view, insets, _ ->
            view.updatePadding(top = insets.systemWindowInsetTop)
        }

        binding.toolbar.setupWithNavController(findNavController())
        binding.toolbar.title = ""

        if (args.isMe) {
            // Inflate edit and settings items
            if (binding.toolbar.menu.isNotEmpty()) return

            binding.toolbar.inflateMenu(R.menu.user_profile_menu)
            binding.toolbar.menu.forEach {
                it.icon?.setTint(requireContext().colorAttr(com.google.android.material.R.attr.colorOnSurface))
                it.icon?.setTintMode(PorterDuff.Mode.SRC_IN)
            }
            binding.toolbar.setOnMenuItemClickListener { item ->
                return@setOnMenuItemClickListener when (item.itemId) {
                    R.id.edit_item -> {
                        onEditClicked()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun onEditClicked() {
        Snackbar.make(requireView(), "This feature is coming soon", Snackbar.LENGTH_LONG)
    }

    private fun setupViewPager(account: Account) {
        val pagerAdapter = object: FragmentStateAdapter(childFragmentManager, lifecycle) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> FeedFragment().apply {
                        arguments = FeedFragmentArgs(feedType = FeedType.AccountToots(
                                accountId = account.accountId,
                                withReplies = false,
                                onlyMedia = false
                        )).toBundle()
                    }
                    1 -> FeedFragment().apply {
                        arguments = FeedFragmentArgs(feedType = FeedType.AccountToots(
                                accountId = account.accountId,
                                withReplies = true,
                                onlyMedia = false
                        )).toBundle()
                    }
                    2 -> FeedFragment().apply {
                        arguments = FeedFragmentArgs(feedType = FeedType.AccountToots(
                                accountId = account.accountId,
                                withReplies = false,
                                onlyMedia = true
                        )).toBundle()
                    }
                    else -> throw IndexOutOfBoundsException("Unknown page index $position")
                }
            }

        }

        binding.pager.adapter = pagerAdapter
        binding.pager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Toots"
                1 -> "w/ replies"
                2 -> "Media"
                else -> throw IndexOutOfBoundsException("Unexpected index")
            }

            binding.pager.setCurrentItem(tab.position, true)
        }.attach()

        binding.pager.adapter = pagerAdapter
    }

    private fun bindNetworkState(networkState: NetworkState) {
        when (networkState) {
            NetworkState.Loading -> {
                TransitionManager.beginDelayedTransition(requireView() as ViewGroup)
                binding.loadingLayout.isVisible = true
                binding.networkOfflineLayout.isVisible = false
            }
            NetworkState.Loaded -> {
                TransitionManager.beginDelayedTransition(requireView() as ViewGroup)
                binding.loadingLayout.isVisible = false
                binding.networkOfflineLayout.isVisible = false
            }
            NetworkState.Offline -> {
                TransitionManager.beginDelayedTransition(requireView() as ViewGroup)
                binding.loadingLayout.isVisible = false
                binding.networkOfflineLayout.isVisible = true
                binding.retryButton.setOnClickListener {
                    viewModel.load()
                }
            }
        }
    }

    private fun bindAccount(account: Account) {
        @ColorInt val color = requireContext().colorAttr(io.github.koss.mammut.base.R.attr.colorPrimaryTransparency)

        // Notification image
        GlideApp.with(binding.profileImageView)
                .load(account.avatar)
                .thumbnail(
                        GlideApp.with(binding.profileImageView)
                                .load(ColorDrawable(color))
                                .apply(RequestOptions.circleCropTransform())
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profileImageView)

        binding.profileImageView.setOnClickListener {
            displayPhoto(binding.profileImageView, account.avatar, customSourceOptions = RequestOptions.circleCropTransform())
        }

        // Header image
        GlideApp.with(binding.coverPhotoImageView)
                .load(account.header)
                .thumbnail(
                        GlideApp.with(binding.coverPhotoImageView)
                                .load(ColorDrawable(color))
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.bitmapTransform(MultiTransformation(BlurTransformation(25), ColorFilterTransformation(color))))
                .into(binding.coverPhotoImageView)

        binding.usernameTextView.text = "@${account.acct}"
        binding.displayNameTextView.text = if (account.displayName.isEmpty()) account.acct else account.displayName
        binding.descriptionTextView.text = HtmlCompat.fromHtml(account.note, HtmlCompat.FROM_HTML_MODE_COMPACT)

        binding.tootCountTextView.text = account.statusesCount.toString()
        binding.followingCountTextView.text = account.followingCount.toString()
        binding.followerCountTextView.text = account.followersCount.toString()

        setupViewPager(account)
    }

    private fun bindFollowButton(followState: FollowState) {
        fun MaterialButton.showLoading() {
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup)
            visibility = View.VISIBLE
            isEnabled = false
        }

        fun MaterialButton.hideLoading() {
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup)
            visibility = View.VISIBLE
            isEnabled = true
        }

        when (followState) {
            is FollowState.Following -> {
                if (followState.loadingUnfollow) {
                    binding.followButton.showLoading()
                } else {
                    binding.followButton.hideLoading()
                }
                binding.followButton.text = requireContext().getString(R.string.unfollow)
            }
            is FollowState.NotFollowing -> {
                if (followState.loadingFollow) {
                    binding.followButton.showLoading()
                } else {
                    binding.followButton.hideLoading()
                }
                binding.followButton.text = view?.context?.getString(R.string.follow)
            }
            FollowState.IsMe -> {
                binding.followButton.visibility = View.GONE
            }
        }

        binding.followButton.setOnClickListener {
            viewModel.requestFollowToggle(followState)
        }
    }

    companion object {

        private val profileOpenCount = AtomicInteger()
    }
}
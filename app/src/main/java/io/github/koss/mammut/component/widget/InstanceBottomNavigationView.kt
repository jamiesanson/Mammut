package io.github.koss.mammut.component.widget

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.github.ajalt.flexadapter.FlexAdapter
import com.github.ajalt.flexadapter.register
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.BuildConfig
import io.github.koss.mammut.R
import io.github.koss.mammut.base.util.GlideApp
import io.github.koss.mammut.base.util.behaviour
import io.github.koss.mammut.data.extensions.fullAcct
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.databinding.InstanceBottomNavigationViewBinding
import io.github.koss.mammut.feature.instance.bottomnav.BottomNavigationViewState
import kotlinx.coroutines.*
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.dimen
import org.jetbrains.anko.dip
import org.jetbrains.anko.displayMetrics

class InstanceBottomNavigationView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null
) : MaterialCardView(context, attributeSet) {

    private val binding = InstanceBottomNavigationViewBinding
            .inflate(LayoutInflater.from(context), this)

    private var peekInsetAddition: Int = 0
    private var peekJob: Job = Job()

    private var currentState: BottomNavigationViewState? = null
    private var initialised: Boolean = false

    var onNavigationClickListener: OnNavigationClickListener? = null
    var onSheetScrollListener: OnSheetScrollListener? = null
    var onInstanceChangeListener: OnInstanceChangeListener? = null

    val navigationView: BottomNavigationView
        get() = binding.bottomNavigationView

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initialise()
    }

    private fun initialise() {
        // Ensure this is being used correctly
        require(layoutParams is CoordinatorLayout.LayoutParams) {
            "InstanceBottomNavigationView must be used within a CoordinatorLayout"
        }

        // Apply insets
        doOnApplyWindowInsets { _, insets, _ ->
            if (insets.systemWindowInsetBottom != 0) {
                peekInsetAddition = insets.systemWindowInsetBottom
                updatePadding(bottom = insets.systemWindowInsetBottom)
                resetPeek()
            }
        }

        // Setup top scrim
        with (binding.bottomNavigationTopScrim) {
            outlineProvider = BottomNavOutlineProvider()
            setOnClickListener {
                when (behaviour<BottomSheetBehavior<View>>()?.state) {
                    BottomSheetBehavior.STATE_EXPANDED -> collapse()
                    BottomSheetBehavior.STATE_COLLAPSED -> expand()
                }
            }
        }

        radius = dip(12f).toFloat()

        // Setup navigation
        binding.settingsCell.setOnClickListener {
            onNavigationClickListener?.onNavigationClicked(NavigationDestination.Settings)
        }

        binding.pendingWorkCell.isVisible = BuildConfig.DEBUG
        binding.pendingWorkCell.setOnClickListener {
            onNavigationClickListener?.onNavigationClicked(NavigationDestination.PendingWork)
        }

        binding.aboutAppCell.setOnClickListener {
            onNavigationClickListener?.onNavigationClicked(NavigationDestination.AboutApp)
        }

        // Setup slide listening
        behaviour<BottomSheetBehavior<View>>()
                ?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(view: View, proportion: Float) {
                        if (peekJob.children.any { it.isActive }) {
                            // Check to see that the user hasn't tried to swipe up the bottom sheet
                            val peekHeight = behaviour<BottomSheetBehavior<View>>()?.peekHeight
                            val screenHeight = view.context.displayMetrics.heightPixels
                            if (peekHeight != null && (screenHeight - view.y) > (view.dimen(R.dimen.profile_cell_height) + peekHeight + peekInsetAddition)) {
                                // Reset peak height and re-enable dimming
                                resetPeek()
                            } else {
                                return
                            }
                        }

                        onSheetScrollListener?.onScrolled(proportion)
                    }

                    override fun onStateChanged(p0: View, p1: Int) {}

                })

        setupInstancesRecycler()

        initialised = true

        // If we've got a state, render it
        currentState?.let {
            renderBottomNavigationContent(it)
        }
    }

    fun collapse() {
        behaviour<BottomSheetBehavior<View>>()
                ?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun expand() {
        behaviour<BottomSheetBehavior<View>>()
                ?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun setState(state: BottomNavigationViewState) {
        currentState = state

        if (initialised) {
            renderBottomNavigationContent(state)
        }
    }

    fun peekCurrentUser() {
        // Change the peek height of the bottom navigation, then change it back
        peekJob.cancelChildren()

        GlobalScope.launch(peekJob) {
            delay(200L)
            val startingHeight = dimen(R.dimen.default_navigation_peek_height)
            val additionalHeight = dimen(R.dimen.profile_cell_height)

            withContext(Dispatchers.Main) {
                // Disable interactions
                updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    (behavior as BottomSheetBehavior).apply {
                        state = BottomSheetBehavior.STATE_EXPANDED
                        peekHeight = startingHeight + additionalHeight + peekInsetAddition
                        state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }

            delay(1500L)

            withContext(Dispatchers.Main) {
                updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    (behavior as BottomSheetBehavior).apply {
                        if (state != BottomSheetBehavior.STATE_COLLAPSED) {
                            peekHeight = startingHeight + peekInsetAddition
                        } else {
                            state = BottomSheetBehavior.STATE_EXPANDED
                            peekHeight = startingHeight + peekInsetAddition
                            state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                }

                // The following delay is such that dimming is now re-enabled until the peek animation concludes
                delay(100)
            }
        }

        peekJob.invokeOnCompletion {
            when (it) {
                is CancellationException -> {
                    // Reset the peek height of the bottom sheet
                    GlobalScope.launch(peekJob) {
                        withContext(Dispatchers.Main) {
                            behaviour<BottomSheetBehavior<View>>()?.peekHeight = dimen(R.dimen.default_navigation_peek_height) + peekInsetAddition
                        }
                    }
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        peekJob.cancelChildren()
    }

    private fun resetPeek() {
        // Reset peak height and re-enable dimming
        val peekHeight = dimen(R.dimen.default_navigation_peek_height) + peekInsetAddition

        if (behaviour<BottomSheetBehavior<View>>()?.peekHeight != peekHeight) {
            behaviour<BottomSheetBehavior<View>>()
                ?.peekHeight = peekHeight
        }

        peekJob.cancelChildren()
    }

    private fun setupInstancesRecycler() {
        @ColorInt val placeholderColor = colorAttr(R.attr.colorOnSurface)

        binding.instancesRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // TODO - re-enable ordering
        //        ItemTouchHelper(InstanceOrderingItemTouchHelper(registrationRepository))
        //                .attachToRecyclerView(binding.instancesRecyclerView)

        if (binding.instancesRecyclerView.onFlingListener == null) {
            LinearSnapHelper()
                    .attachToRecyclerView(binding.instancesRecyclerView)
        }

        // Setup flex adapter
        binding.instancesRecyclerView.adapter = FlexAdapter<Pair<Account, Boolean>>().apply {
            register<Pair<Account, Boolean>>(layout = R.layout.card_account) { (account, selected), view, index ->
                (view as MaterialCardView).apply {
                    if (selected) {
                        strokeWidth = dip(2)
                    } else {
                        strokeWidth = 0
                        setOnClickListener {
                            onInstanceChangeListener?.onInstanceChanged(index)
                            collapse()
                        }
                    }
                }

                view.findViewById<TextView>(R.id.displayNameTextView).text = account.displayName
                view.findViewById<TextView>(R.id.usernameTextView).apply {
                    text = account.acct
                    isSelected = true
                }

                GlideApp.with(view)
                        .load(account.header)
                        .thumbnail(
                                GlideApp.with(view)
                                        .load(ColorDrawable(placeholderColor))
                        )
                        .apply(RequestOptions.centerCropTransform())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(view.findViewById(R.id.backgroundImageView))

                GlideApp.with(view)
                        .load(account.avatar)
                        .thumbnail(
                                GlideApp.with(view)
                                        .load(ColorDrawable(placeholderColor))
                                        .apply(RequestOptions.circleCropTransform())
                        )
                        .apply(RequestOptions.circleCropTransform())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(view.findViewById(R.id.avatarImageView))
            }

        }
    }

    private fun renderBottomNavigationContent(state: BottomNavigationViewState) {
        // Load Account
        @ColorInt val placeholderColor = colorAttr(R.attr.colorOnSurface)

        GlideApp.with(this)
                .load(state.currentUser.avatar)
                .thumbnail(
                        GlideApp.with(this)
                                .load(ColorDrawable(placeholderColor))
                                .apply(RequestOptions.circleCropTransform())
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profileImageView)

        val selectedItemList = state.allAccounts.map { it.accountCreatedAt == state.currentUser.accountCreatedAt }

        @Suppress("UNCHECKED_CAST")
        (binding.instancesRecyclerView.adapter as FlexAdapter<Pair<Account, Boolean>>).let {
            if (it.items.size == 0) {
                it.resetItems(state.allAccounts.zip(selectedItemList))
            } else {
                val diff = DiffUtil.calculateDiff(getInstancesDiffCallback(it.items.map { it.first }, state.allAccounts.toList()), true)
                diff.dispatchUpdatesTo(it)

                // Find selected account and scroll to it
                val selectedIndex = state.allAccounts.indexOfFirst { it.accountCreatedAt == state.currentUser.accountCreatedAt }
                binding.instancesRecyclerView.scrollToPosition(selectedIndex)
            }
        }

        with(binding) {
            displayNameTextView.text = state.currentUser.displayName
            usernameTextView.text = state.currentUser.fullAcct(state.instanceName)

            // Find selected account and scroll to it
            val selectedIndex = state.allAccounts.indexOfFirst { it.accountCreatedAt == state.currentUser.accountCreatedAt }
            instancesRecyclerView.scrollToPosition(selectedIndex)

            addAccountButton.setOnClickListener {
                collapse()
                onNavigationClickListener?.onNavigationClicked(NavigationDestination.JoinInstance)
            }

            profileCell.setOnClickListener {
                collapse()
                onNavigationClickListener?.onNavigationClicked(NavigationDestination.Profile)
            }
        }
    }

    private fun getInstancesDiffCallback(oldList: List<Account>, newList: List<Account>): DiffUtil.Callback = object: DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition].accountId == newList[newItemPosition].accountId

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldList[oldItemPosition] == newList[newItemPosition]
    }

    /**
     * Enum defining destinations a user can navigate to
     */
    enum class NavigationDestination {
        Settings,
        PendingWork,
        AboutApp,
        JoinInstance,
        Profile
    }

    /**
     * Listener called when the instance is changed
     */
    interface OnInstanceChangeListener {
        fun onInstanceChanged(index: Int)
    }

    /**
     * Listener called when navigation is actioned
     */
    interface OnNavigationClickListener {
        fun onNavigationClicked(destination: NavigationDestination)
    }

    /**
     * Listener called when the bottom sheet is scrolled
     */
    interface OnSheetScrollListener {
        fun onScrolled(proportion: Float)
    }

    private class BottomNavOutlineProvider: ViewOutlineProvider() {

        override fun getOutline(view: View?, outline: Outline?) {
            outline?.setRoundRect(0,0, view!!.width, (view.height + view.context.dip(12)), view.context.dip(12).toFloat())
        }
    }
}
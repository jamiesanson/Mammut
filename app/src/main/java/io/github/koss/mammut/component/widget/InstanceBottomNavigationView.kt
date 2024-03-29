package io.github.koss.mammut.component.widget

import android.app.Activity
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.Px
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.window.layout.WindowMetricsCalculator
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.github.ajalt.flexadapter.FlexAdapter
import com.github.ajalt.flexadapter.register
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.BuildConfig
import io.github.koss.mammut.R
import io.github.koss.mammut.base.anko.colorAttr
import io.github.koss.mammut.base.anko.dimen
import io.github.koss.mammut.base.anko.dip
import io.github.koss.mammut.base.util.GlideApp
import io.github.koss.mammut.base.util.behaviour
import io.github.koss.mammut.data.extensions.fullAcct
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.databinding.InstanceBottomNavigationViewBinding
import io.github.koss.mammut.feature.home.presentation.state.HomeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstanceBottomNavigationView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null
) : MaterialCardView(context, attributeSet) {

    private val binding = InstanceBottomNavigationViewBinding
            .inflate(LayoutInflater.from(context), this)

    private var peekInsetAddition: Int  = 0

    private var peekJob = Job()

    private var currentState: HomeState? = null
    private var initialised: Boolean = false

    var onNavigationClickListener: OnNavigationClickListener? = null
    var onSheetScrollListener: OnSheetScrollListener? = null
    var onInstanceChangeListener: OnInstanceChangeListener? = null

    val navigationView: BottomNavigationView
        get() = binding.bottomNavigationView

    fun initialise() {
        // Ensure this is being used correctly
        require(layoutParams is CoordinatorLayout.LayoutParams) {
            "InstanceBottomNavigationView must be used within a CoordinatorLayout"
        }

        // Setup top scrim
        with (binding.bottomNavigationTopScrim) {
            outlineProvider = BottomNavOutlineProvider()
            setOnClickListener {
                when (behaviour<BottomSheetBehavior<View>>()?.state) {
                    BottomSheetBehavior.STATE_EXPANDED -> collapse()
                    BottomSheetBehavior.STATE_COLLAPSED -> expand()
                    else -> {}
                }
            }
        }

        val cornerRadius = context.dip(12f).toFloat()

        shapeAppearanceModel = ShapeAppearanceModel.Builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                .build()

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
        behaviour<BottomSheetBehavior<View>>()?.apply {
           // isGestureInsetBottomIgnored = true

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(view: View, proportion: Float) {
                    if (peekJob.children.any { it.isActive }) {
                        // Check to see that the user hasn't tried to swipe up the bottom sheet
                        val peekHeight = behaviour<BottomSheetBehavior<View>>()?.peekHeight
                        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(view.context as Activity)
                        val screenHeight = metrics.bounds.height()
                        if (peekHeight != null && (screenHeight - view.y) > (view.context.dimen(R.dimen.profile_cell_height) + peekHeight + peekInsetAddition)) {
                            // Reset peak height and re-enable dimming
                            resetPeek()
                        } else {
                            return
                        }
                    }

                    // Animate change in padding
                    val bottomNavPaddingBottom = (peekHeight * (1 - proportion)) / 2f
                    val aboutAppMarginBottom = (peekHeight * proportion) / 2f

                    navigationView.updatePadding(bottom = bottomNavPaddingBottom.toInt())
                    binding.aboutAppCell.updateLayoutParams<MarginLayoutParams> { bottomMargin = aboutAppMarginBottom.toInt() }

                    onSheetScrollListener?.onScrolled(proportion)
                }

                override fun onStateChanged(p0: View, p1: Int) {}

            })
        }

        setupInstancesRecycler()

        // Setup peek
        doOnApplyWindowInsets { _, insets, _ ->
            peekInsetAddition = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        }
        resetPeek()

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

    fun setState(state: HomeState) {
        currentState = state

        if (initialised) {
            renderBottomNavigationContent(state)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun peekCurrentUser() {
        // Change the peek height of the bottom navigation, then change it back
        peekJob.cancelChildren()

        GlobalScope.launch(peekJob) {
            delay(200L)
            val startingHeight = context.dimen(R.dimen.default_navigation_peek_height)
            val additionalHeight = context.dimen(R.dimen.profile_cell_height)

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
                            behaviour<BottomSheetBehavior<View>>()?.peekHeight = context.dimen(R.dimen.default_navigation_peek_height) + peekInsetAddition
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
        val peekHeight = context.dimen(R.dimen.default_navigation_peek_height) + peekInsetAddition

        if (behaviour<BottomSheetBehavior<View>>()?.peekHeight != peekHeight) {
            behaviour<BottomSheetBehavior<View>>()
                ?.peekHeight = peekHeight
        }

        peekJob.cancelChildren()
    }

    private fun setupInstancesRecycler() {
        @ColorInt val placeholderColor = context.colorAttr(com.google.android.material.R.attr.colorOnSurface)

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
                        strokeWidth = context.dip(2f)
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

    private fun renderBottomNavigationContent(state: HomeState) {
        // Load Account
        @ColorInt val placeholderColor = context.colorAttr(com.google.android.material.R.attr.colorOnSurface)

        GlideApp.with(this)
                .load(state.currentUser!!.avatar)
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
            outline?.setRoundRect(0,0, view!!.width, (view.height + view.context.dip(12f)), view.context.dip(12f).toFloat())
        }
    }
}
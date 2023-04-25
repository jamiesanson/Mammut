package io.github.koss.mammut.feed.ui.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.getSystemService
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import io.github.koss.mammut.base.anko.dip
import io.github.koss.mammut.base.util.inflate
import io.github.koss.mammut.base.util.observe
import io.github.koss.mammut.base.util.postIfMutable
import io.github.koss.mammut.base.widget.ExpandableFloatingActionButton
import io.github.koss.mammut.feed.R

/**
 * General network indicator class - can be attached to any `FrameLayout`-esque layout
 */
class NetworkIndicator {

    private val receiver = NetworkIndicatorReceiver()

    private val isConnectedLiveData: LiveData<Boolean> = MutableLiveData()

    fun attach(view: ViewGroup, lifecycleOwner: LifecycleOwner) {
        if (view.findViewById<View>(R.id.offlineModeButton) == null) {
            view.inflate(R.layout.button_network_indicator, addToRoot = true)
            view.context.registerReceiver(NetworkIndicatorReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }

        view.doOnApplyWindowInsets { _, insets, initialState ->
            view.findViewById<View>(R.id.offlineModeButton)?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = initialState.margins.top + insets.systemWindowInsetTop
            }
        }

        lifecycleOwner.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                view.context.unregisterReceiver(receiver)
            }
        })

        // Observe network state
        val offlineModeButton = view.findViewById<ExpandableFloatingActionButton>(R.id.offlineModeButton)
        offlineModeButton.doOnLayout {
            offlineModeButton.apply {
                translationY = if (isConnectedLiveData.value != false) {
                    -(y + height + view.context.dip(50f))
                } else {
                    0f.also {
                        offlineModeButton.postDelayed({
                            offlineModeButton.collapse(300L)
                        }, 3000L)
                    }
                }
            }

            isConnectedLiveData.observe(lifecycleOwner) {
                when {
                    !it -> {
                        if (offlineModeButton.translationY != 0F) {
                            // Animate in from top
                            offlineModeButton.animate()
                                    .translationY(0F)
                                    .setInterpolator(OvershootInterpolator())
                                    .setDuration(300L)
                                    .start()

                            offlineModeButton.postDelayed({
                                offlineModeButton.collapse(300L)
                            }, 3000L)
                        }

                        offlineModeButton.apply {
                            setOnClickListener {
                                if (isExpanded) {
                                    collapse(300)
                                } else {
                                    expand(300)
                                }
                            }
                        }
                    }
                    else -> {
                        if (offlineModeButton.translationY == 0F) {
                            // Animate out
                            offlineModeButton.animate()
                                    .translationY(-(offlineModeButton.y + offlineModeButton.height - 50))
                                    .setInterpolator(AccelerateInterpolator())
                                    .setDuration(150L)
                                    .start()
                        }
                    }
                }

            }
        }
    }

    inner class NetworkIndicatorReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context?.getSystemService<ConnectivityManager>()?.activeNetworkInfo?.isConnected == true) {
                isConnectedLiveData.postIfMutable(true)
            } else {
                isConnectedLiveData.postIfMutable(false)
            }
        }
    }
}
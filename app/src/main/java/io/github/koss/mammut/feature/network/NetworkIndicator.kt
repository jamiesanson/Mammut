package io.github.koss.mammut.feature.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.koss.mammut.R
import io.github.koss.mammut.base.util.inflate
import io.github.koss.mammut.extension.observe
import io.github.koss.mammut.extension.postSafely
import kotlinx.android.synthetic.main.button_network_indicator.view.*
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * General network indicator class - can be attached to any `FrameLayout`-esque layout
 */
class NetworkIndicator(context: Context) {

    init {
        @Suppress("DEPRECATION") // Deprecation suppressed here as this is the most simple solution for now.
        context.registerReceiver(NetworkIndicatorReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    private val isConnectedLiveData: LiveData<Boolean> = MutableLiveData()

    fun attach(view: ViewGroup, lifecycleOwner: LifecycleOwner) {
        if (view.offlineModeButton == null) {
            view.inflate(R.layout.button_network_indicator, addToRoot = true)
        }

        // Observe network state
        view.offlineModeButton.doOnLayout {
            view.offlineModeButton.apply {
                translationY = if (isConnectedLiveData.value != false) {
                    -(y + height + 50)
                } else {
                    0f.also {
                        view.offlineModeButton.postDelayed({
                            view.offlineModeButton.collapse(300L)
                        }, 3000L)
                    }
                }
            }

            isConnectedLiveData.observe(lifecycleOwner) {
                when {
                    !it -> {
                        if (view.offlineModeButton.translationY != 0F) {
                            // Animate in from top
                            view.offlineModeButton.animate()
                                    .translationY(0F)
                                    .setInterpolator(OvershootInterpolator())
                                    .setDuration(300L)
                                    .start()

                            view.offlineModeButton.postDelayed({
                                view.offlineModeButton.collapse(300L)
                            }, 3000L)
                        }

                        view.offlineModeButton.apply {
                            onClick {
                                if (isExpanded) {
                                    collapse(300)
                                } else {
                                    expand(300)
                                }
                            }
                        }
                    }
                    else -> {
                        if (view.offlineModeButton.translationY == 0F) {
                            // Animate out
                            view.offlineModeButton.animate()
                                    .translationY(-(view.offlineModeButton.y + view.offlineModeButton.height + 50))
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
            if (context?.connectivityManager?.activeNetworkInfo?.isConnected == true) {
                isConnectedLiveData.postSafely(true)
            } else {
                isConnectedLiveData.postSafely(false)
            }
        }
    }
}
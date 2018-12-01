package io.github.jamiesanson.mammut.feature.network

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
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.extension.inflate
import io.github.jamiesanson.mammut.extension.observe
import io.github.jamiesanson.mammut.extension.postSafely
import kotlinx.android.synthetic.main.button_network_indicator.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * General network indicator class - can be attached to any `FrameLayout`-esque layout
 */
class NetworkIndicator(context: Context) {

    init {
        context.registerReceiver(NetworkIndicatorReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    private val isConnectedLiveData: LiveData<Boolean> = MutableLiveData()

    fun attach(view: ViewGroup, lifecycleOwner: LifecycleOwner) {
        if (view.offlineModeButton == null) {
            view.inflate(R.layout.button_network_indicator, addToRoot = true)
        }

        // Observe network state
        view.offlineModeButton.doOnLayout { _ ->

            view.offlineModeButton.apply {
                translationY = if (isConnectedLiveData.value == true) {
                    -(y + height)
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
                                    .translationY(-(view.offlineModeButton.y + view.offlineModeButton.height))
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
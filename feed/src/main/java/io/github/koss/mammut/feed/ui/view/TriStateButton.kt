package io.github.koss.mammut.feed.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import io.github.koss.mammut.base.anko.colorAttr
import io.github.koss.mammut.base.anko.dip
import io.github.koss.mammut.feed.R
import io.github.koss.mammut.feed.databinding.ViewTristateButtonBinding

/**
 * View subclass for handling a nice button animation when submitting boosts and retoots
 */
class TriStateButton @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attributeSet, defStyleAttr) {

    private val binding: ViewTristateButtonBinding

    enum class State {
        ACTIVE,
        INACTIVE,
        PENDING
    }

    var currentState = State.ACTIVE
        private set

    var text: String = ""
        set(text) {
            ViewTristateButtonBinding.bind(this).textView.text = text
            field = text
        }

    private lateinit var activeDrawable: Drawable
    private lateinit var inactiveDrawable: Drawable

    init {
        binding = ViewTristateButtonBinding.inflate(LayoutInflater.from(context), this, true)

        // Resolve drawables
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.TriStateButton, 0, 0).use {
            activeDrawable = it.getDrawable(R.styleable.TriStateButton_activeIcon)
                ?: throw IllegalArgumentException("activeIcon required")

            inactiveDrawable = it.getDrawable(R.styleable.TriStateButton_inactiveIcon)
                ?: throw IllegalArgumentException("activeIcon required")
        }

        binding.imageView.setImageDrawable(activeDrawable)
        binding.imageView.setColorFilter(context.colorAttr(com.google.android.material.R.attr.colorAccent))
        binding.progressBar.isVisible = false
    }

    fun updateState(newState: State, animate: Boolean = true) {
        if (newState == currentState) return

        if (animate) TransitionManager.beginDelayedTransition(this, AutoTransition())

        if (newState == State.PENDING) {
            // Disable clicking
            isEnabled = false

            binding.progressBar.visibility = View.VISIBLE
            binding.imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = context.dip(16f)
                width = context.dip(16f)
            }
        }

        if (newState == State.ACTIVE) {
            // Disable clicking
            isEnabled = true

            with(binding) {
                progressBar.visibility = View.INVISIBLE
                imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    height = context.dip(24f)
                    width = context.dip(24f)
                }
                imageView.setImageDrawable(activeDrawable)
                imageView.setColorFilter(context.colorAttr(com.google.android.material.R.attr.colorAccent))
            }
        }

        if (newState == State.INACTIVE) {
            // Disable clicking
            isEnabled = true

            with(binding) {
                progressBar.visibility = View.INVISIBLE
                imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    height = context.dip(24f)
                    width = context.dip(24f)
                }
                imageView.setImageDrawable(inactiveDrawable)
                imageView.setColorFilter(context.colorAttr(io.github.koss.mammut.base.R.attr.colorControlNormalTransparent))
            }
        }

        currentState = newState
    }
}
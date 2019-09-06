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
import io.github.koss.mammut.feed.R
import kotlinx.android.synthetic.main.view_tristate_button.view.*
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.dip

/**
 * View subclass for handling a nice button animation when submitting boosts and retoots
 */
class TriStateButton @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attributeSet, defStyleAttr) {

    enum class State {
        ACTIVE,
        INACTIVE,
        PENDING
    }

    var currentState = State.ACTIVE
        private set

    var text: String = ""
        set(text) {
            textView.text = text
            field = text
        }

    private lateinit var activeDrawable: Drawable
    private lateinit var inactiveDrawable: Drawable

    init {
        LayoutInflater
            .from(context)
            .inflate(R.layout.view_tristate_button, this, true)

        // Resolve drawables
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.TriStateButton, 0, 0).use {
            activeDrawable = it.getDrawable(R.styleable.TriStateButton_activeIcon)
                ?: throw IllegalArgumentException("activeIcon required")

            inactiveDrawable = it.getDrawable(R.styleable.TriStateButton_inactiveIcon)
                ?: throw IllegalArgumentException("activeIcon required")
        }

        imageView.setImageDrawable(activeDrawable)
        progressBar.isVisible = false
    }

    fun updateState(newState: State, animate: Boolean = true) {
        if (newState == currentState) return

        if (animate) TransitionManager.beginDelayedTransition(this, AutoTransition())

        if (newState == State.PENDING) {
            // Disable clicking
            isEnabled = false

            progressBar.visibility = View.VISIBLE
            imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = dip(16)
                width = dip(16)
            }
        }

        if (newState == State.ACTIVE) {
            // Disable clicking
            isEnabled = true

            progressBar.visibility = View.INVISIBLE
            imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = dip(24)
                width = dip(24)
            }
            imageView.setImageDrawable(activeDrawable)
            imageView.setColorFilter(colorAttr(R.attr.colorAccent))
        }

        if (newState == State.INACTIVE) {
            // Disable clicking
            isEnabled = true

            progressBar.visibility = View.INVISIBLE
            imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = dip(24)
                width = dip(24)
            }
            imageView.setImageDrawable(inactiveDrawable)
            imageView.setColorFilter(colorAttr(R.attr.colorControlNormalTransparent))
        }

        currentState = newState
    }
}
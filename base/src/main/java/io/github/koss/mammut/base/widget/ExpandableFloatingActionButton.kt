package io.github.koss.mammut.base.widget

import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.Outline
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionManager
import io.github.koss.mammut.base.R
import kotlinx.android.synthetic.main.button_expandable_fab.view.*
import org.jetbrains.anko.textColor

/**
 * Floating action button containing "Add DrinkTable" text
 */
class ExpandableFloatingActionButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private var animating: Boolean = false

    var isExpanded: Boolean = true
        private set

    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.ExpandableFloatingActionButton,
                0, 0).apply {

            if (getBoolean(R.styleable.ExpandableFloatingActionButton_small, false)) {
                LayoutInflater.from(context).inflate(R.layout.button_expandable_fab_small, this@ExpandableFloatingActionButton, true)
            } else {
                LayoutInflater.from(context).inflate(R.layout.button_expandable_fab, this@ExpandableFloatingActionButton, true)
            }

            try {
                expandableFabTextView.text = getString(R.styleable.ExpandableFloatingActionButton_buttonText)?.capitalize()
                val color = getColor(R.styleable.ExpandableFloatingActionButton_buttonAccentColor,
                        ContextCompat.getColor(context, android.R.color.white))

                expandableFabTextView.textColor = color
                expandableFabImageView.setImageDrawable(getDrawable(R.styleable.ExpandableFloatingActionButton_buttonIcon))
                expandableFabImageView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            } finally {
                recycle()
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val viewOutlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height.toFloat() / 2)
            }
        }
        outlineProvider = viewOutlineProvider
        clipToOutline = true
        isClickable = true
        isFocusable = true
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.animator.expandable_fab_state_list_animator)
    }

    fun expand(duration: Long) {
        if (!animating) {
            TransitionManager.beginDelayedTransition(this, ChangeBounds().apply {
                setDuration(duration)
                addListener(CollapseListener())
                interpolator = AccelerateDecelerateInterpolator()
            })
            TransitionManager.beginDelayedTransition(constraintLayout, AutoTransition().apply {
                setDuration(duration)
                addListener(CollapseListener())
                interpolator = AccelerateDecelerateInterpolator()
            })

            expandableFabTextView.visibility = View.VISIBLE
            isExpanded = true
        }
    }

    fun collapse(duration: Long) {
        if (!animating) {
            TransitionManager.beginDelayedTransition(this, ChangeBounds().apply {
                setDuration(duration)
                addListener(CollapseListener())
                interpolator = AccelerateDecelerateInterpolator()
            })
            TransitionManager.beginDelayedTransition(constraintLayout, AutoTransition().apply {
                setDuration(duration)
                addListener(CollapseListener())
                interpolator = AccelerateDecelerateInterpolator()
            })

            expandableFabTextView.visibility = View.GONE
            isExpanded = false
        }
    }

    inner class CollapseListener: Transition.TransitionListener {
        override fun onTransitionEnd(transition: Transition) {
            animating = false
            transition.removeListener(this)
        }

        override fun onTransitionResume(transition: Transition) {}

        override fun onTransitionPause(transition: Transition) {}

        override fun onTransitionCancel(transition: Transition) {
            animating = false
            transition.removeListener(this)
        }

        override fun onTransitionStart(transition: Transition) {
            animating = true
        }
    }
}
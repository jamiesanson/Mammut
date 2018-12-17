package io.github.koss.mammut.component.widget.scrollintercepting

import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import java.lang.Math.abs


class ScrollInterceptionBehavior : CoordinatorLayout.Behavior<View>() {

    private var onScroll: ((Direction) -> Unit)? = null
    private var accumulatedScrollOffsetDown = 0
    private var accumulatedScrollOffsetUp = 0

    private val scrollSensitivityUp = 30
    private val scrollSensitivityDown = 100

    fun scrollCallback(onScroll: (Direction) -> Unit) {
        this.onScroll = onScroll
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        return dependency is FrameLayout
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View, target: View, nestedScrollAxes: Int, type: Int): Boolean {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (dy < 0) {
            if (accumulatedScrollOffsetUp >= scrollSensitivityUp) {
                onScroll?.invoke(Direction.UP)
            } else {
                accumulatedScrollOffsetUp += abs(dy)
            }

            accumulatedScrollOffsetDown = 0
        }

        if (dy > 0) {
            if (accumulatedScrollOffsetDown >= scrollSensitivityDown) {
                onScroll?.invoke(Direction.DOWN)
            } else {
                accumulatedScrollOffsetDown += dy
            }

            accumulatedScrollOffsetUp = 0
        }
    }
}
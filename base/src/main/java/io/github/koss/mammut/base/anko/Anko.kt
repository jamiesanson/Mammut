package io.github.koss.mammut.base.anko

import android.R
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.Dimension
import androidx.annotation.Px
import kotlin.math.roundToInt


@Px
fun Context.dimen(@DimenRes id: Int): Int {
    val dp = resources.getDimension(id)
    return (dp * (resources.displayMetrics.density / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
}

@ColorInt
fun Context.colorAttr(@AttrRes id: Int, theme: Resources.Theme = getTheme()): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(id, typedValue, true)

    return typedValue.data
}


@Dimension
fun Context.dimenAttr(@DimenRes id: Int): Int {
    val typedValue = theme.obtainStyledAttributes(intArrayOf(id))
    val size = typedValue.getDimensionPixelSize(0, -1)
    typedValue.recycle()

    return size
}

@Px
fun Context.dip(value: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        resources.displayMetrics
    ).roundToInt()
}
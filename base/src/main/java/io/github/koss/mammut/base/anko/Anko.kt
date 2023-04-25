package io.github.koss.mammut.base.anko

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
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

@Px
fun Context.dip(value: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        resources.displayMetrics
    ).roundToInt()
}
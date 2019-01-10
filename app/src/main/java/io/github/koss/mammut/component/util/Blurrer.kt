package io.github.koss.mammut.component.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View

object Blurrer {

    private lateinit var renderScript: RenderScript

    fun initialise(context: Context) {
        renderScript = RenderScript.create(context)
    }

    fun blurView(view: View, radius: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val input = Allocation.createFromBitmap(renderScript, bitmap)
        val output = Allocation.createTyped(renderScript, input.type)
        val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(bitmap)

        return bitmap
    }
}
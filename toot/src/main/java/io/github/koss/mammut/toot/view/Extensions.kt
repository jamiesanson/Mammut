package io.github.koss.mammut.toot.view

import android.text.Spannable
import android.text.style.ImageSpan
import android.widget.EditText
import androidx.core.text.getSpans

fun EditText.update(text: Spannable) {
    when {
        this.text.toString() != text.toString() -> {
            // Move the selection by the difference in length of the two strings
            val lengthDifference = text.length - length()
            val previousSelection = selectionStart

            setText(text)
            setSelection(previousSelection + lengthDifference)
        }
        // If we have a differing amount of image spans, set the span on the edit text
        this.text.getSpans<ImageSpan>().size != text.getSpans<ImageSpan>().size -> {
            val previousSelection = selectionStart

            setText(text)
            setSelection(previousSelection)
        }
    }
}
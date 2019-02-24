package io.github.koss.mammut.toot.view

import android.text.Spannable
import android.widget.EditText

fun EditText.update(text: Spannable) {
    if (this.text.toString() != text.toString()) {
        // Move the selection by the difference in length of the two strings
        val lengthDifference = text.length - length()
        val previousSelection = selectionStart

        setText(text)
        setSelection(previousSelection + lengthDifference)
    }
}
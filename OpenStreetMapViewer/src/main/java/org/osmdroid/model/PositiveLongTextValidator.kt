package org.osmdroid.model

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * created on 12/4/2016.
 *
 * @author Alex O'Ree
 * @since 5.6.1
 */
class PositiveLongTextValidator(var parent: EditText, var minValue: Long) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        val txt = parent.getText().toString()
        if (txt == null || txt.length == 0) parent.setError("Not a valid number")
        try {
            val `val` = txt.toLong()
            if (`val` < minValue) {
                parent.setError("Must be at least " + minValue)
            } else {
                parent.setError(null)
            }
        } catch (ex: Exception) {
            parent.setError("Not a valid number")
        }
    }
}

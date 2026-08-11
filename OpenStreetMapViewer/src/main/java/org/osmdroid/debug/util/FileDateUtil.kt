package org.osmdroid.debug.util

import android.annotation.TargetApi
import android.os.Build
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * from http://stackoverflow.com/a/38024962/1203182
 *
 * @since 5.6.2
 */
object FileDateUtil {
    fun getModifiedDate(modified: Long): String {
        return getModifiedDate(Locale.getDefault(), modified)
    }

    fun getModifiedDate(locale: Locale?, modified: Long): String {
        var dateFormat: SimpleDateFormat? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            dateFormat = SimpleDateFormat(getDateFormat(locale))
        } else {
            dateFormat = SimpleDateFormat("MMM/dd/yyyy hh:mm:ss aa")
        }

        return dateFormat.format(Date(modified))
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun getDateFormat(locale: Locale?): String? {
        return DateFormat.getBestDateTimePattern(locale, "MM/dd/yyyy hh:mm:ss aa")
    }
}

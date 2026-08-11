/*
 * WARNING, All test cases exist in osmdroid-android-it/src/main/java (maven project)
 *
 * During build time (with gradle), these tests are copied from osmdroid-android-it to OpenStreetMapViewer/src/androidTest/java
 * DO NOT Modify files in OpenSteetMapViewer/src/androidTest. You will loose your changes when building!
 *
 */
package org.osmdroid.test

import android.widget.TextView
import androidx.test.rule.ActivityTestRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.osmdroid.debug.CacheAnalyzerActivity
import org.osmdroid.tileprovider.util.Counters

class CacheAnalyzerTest {
    @get:Rule
    val activityRule = ActivityTestRule(CacheAnalyzerActivity::class.java)

    @Test
    fun testActivity() {
        Counters.reset()
        val activity = activityRule.activity
        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        activity.runOnUiThread {
            val cacheStats = activity.findViewById<TextView>(org.osmdroid.R.id.cacheStats)
            val text = cacheStats.text.toString()
            Assert.assertNotEquals(text, activity.getString(org.osmdroid.R.string.loading_stats))
            activity.finish()
        }
    }
}

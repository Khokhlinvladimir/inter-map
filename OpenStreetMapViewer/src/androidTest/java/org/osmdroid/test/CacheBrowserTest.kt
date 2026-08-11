/*
 * WARNING, All test cases exist in osmdroid-android-it/src/main/java (maven project)
 *
 * During build time (with gradle), these tests are copied from osmdroid-android-it to OpenStreetMapViewer/src/androidTest/java
 * DO NOT Modify files in OpenSteetMapViewer/src/androidTest. You will loose your changes when building!
 *
 */
package org.osmdroid.test

import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.osmdroid.debug.browser.CacheBrowserActivity
import org.osmdroid.tileprovider.util.Counters

class CacheBrowserTest {
    @get:Rule
    val activityRule = ActivityTestRule(CacheBrowserActivity::class.java)

    @Test
    fun testActivity() {
        Counters.reset()
        val activity = activityRule.activity
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        activity.finish()
    }
}

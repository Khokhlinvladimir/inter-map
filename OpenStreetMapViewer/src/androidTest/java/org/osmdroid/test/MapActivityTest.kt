/*
 * WARNING, All test cases exist in osmdroid-android-it/src/main/java (maven project)
 *
 * During build time (with gradle), these tests are copied from osmdroid-android-it to OpenStreetMapViewer/src/androidTest/java
 * DO NOT Modify files in OpenSteetMapViewer/src/androidTest. You will loose your changes when building!
 *
 */
package org.osmdroid.test

import androidx.test.rule.ActivityTestRule
import junit.framework.Assert
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.osmdroid.StarterMapActivity
import org.osmdroid.tileprovider.util.Counters

class MapActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule(StarterMapActivity::class.java)

    @Test
    fun testActivity() {
        Counters.reset()
        val activity = activityRule.activity
        assertNotNull(activity)
        try {
            Thread.sleep(5000)
        } catch (_: InterruptedException) {
        }
        Counters.printToLogcat()
        if (Counters.countOOM > 0 || Counters.fileCacheOOM > 0) {
            Assert.fail("OOM Detected, aborting!")
        }
        activity.finish()
    }
}

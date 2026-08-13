/*
 * WARNING, All test cases exist in osmdroid-android-it/src/main/java (maven project)
 *
 * During build time (with gradle), these tests are copied from osmdroid-android-it to OpenStreetMapViewer/src/androidTest/java
 * DO NOT Modify files in OpenSteetMapViewer/src/androidTest. You will loose your changes when building!
 *
 */
package org.osmdroid.test

import androidx.test.rule.ActivityTestRule
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.osmdroid.MainActivity
import org.osmdroid.tileprovider.util.Counters

class MainActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testActivity() {
        Counters.reset()
        val activity = activityRule.activity
        assertNotNull(activity)
        Counters.printToLogcat()
        if (Counters.countOOM > 0 || Counters.fileCacheOOM > 0) {
            Assert.fail("OOM Detected, aborting!")
        }
        activity.finish()
    }
}

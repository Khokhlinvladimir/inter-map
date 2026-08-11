/*
 * WARNING, All test cases exist in osmdroid-android-it/src/main/java (maven project)
 *
 * During build time (with gradle), these tests are copied from osmdroid-android-it to OpenStreetMapViewer/src/androidTest/java
 * DO NOT Modify files in OpenSteetMapViewer/src/androidTest. You will loose your changes when building!
 *
 */
package org.osmdroid.test

import android.util.Log
import androidx.test.rule.ActivityTestRule
import junit.framework.Assert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.osmdroid.ExtraSamplesActivity
import org.osmdroid.ISampleFactory
import org.osmdroid.OsmApplication
import org.osmdroid.bugtestfragments.BugFactory
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.SampleFactory
import org.osmdroid.samplefragments.ui.SamplesMenuFragment
import org.osmdroid.tileprovider.util.Counters
import java.util.Random

class ExtraSamplesTest {
    @get:Rule
    val activityRule = ActivityTestRule(ExtraSamplesActivity::class.java)

    private var ok = true

    @Test
    fun testActivity() {
        executeTest(SampleFactory.instance)
    }

    @Test
    fun testBugsDriversActivity() {
        executeTest(BugFactory.instance)
    }

    private fun executeTest(sampleFactory: ISampleFactory) {
        Counters.reset()
        val activity = activityRule.activity
        assertNotNull(activity)
        val fragmentManager = activity.supportFragmentManager
        val fragment = fragmentManager.findFragmentByTag(ExtraSamplesActivity.SAMPLES_FRAGMENT_TAG)
        assertNotNull(fragment)
        assertTrue(fragment is SamplesMenuFragment)

        val fireOrder = IntArray(sampleFactory.count()) { it }
        shuffleArray(fireOrder)

        Log.i(
            SamplesMenuFragment.TAG,
            "Memory allocation: INIT Free: ${Runtime.getRuntime().freeMemory()} " +
                "Total:${Runtime.getRuntime().totalMemory()} Max:${Runtime.getRuntime().maxMemory()}"
        )
        for (index in fireOrder.indices) {
            if (index > 60) break

            for (run in 0 until 1) {
                Log.i(
                    SamplesMenuFragment.TAG,
                    "${run}Memory allocation: Before load: Free: ${Runtime.getRuntime().freeMemory()} " +
                        "Total:${Runtime.getRuntime().totalMemory()} Max:${Runtime.getRuntime().maxMemory()}"
                )
                val sample = sampleFactory.getSample(fireOrder[index])!!
                if (sample.skipOnCiTests()) break

                Log.i(
                    SamplesMenuFragment.TAG,
                    "loading fragment ($index/${sampleFactory.count()}) run $run ${sample.sampleTitle}, " +
                        fragment!!::class.java.canonicalName
                )
                Counters.printToLogcat()
                if (Counters.countOOM > 0 || Counters.fileCacheOOM > 0) {
                    OsmApplication.writeHprof()
                    Assert.fail(
                        "OOM Detected, aborting! this test run was ${sample.sampleTitle}, " +
                            "${sample::class.java.canonicalName} iteration $run"
                    )
                }

                activity.runOnUiThread {
                    try {
                        fragmentManager.beginTransaction()
                            .replace(
                                org.osmdroid.R.id.samples_container,
                                sample,
                                ExtraSamplesActivity.SAMPLES_FRAGMENT_TAG
                            )
                            .addToBackStack(ExtraSamplesActivity.SAMPLES_FRAGMENT_TAG)
                            .commit()
                        fragmentManager.executePendingTransactions()
                    } catch (error: Exception) {
                        ok = false
                        error.printStackTrace()
                        Assert.fail(
                            "Error popping fragment ${sample.sampleTitle}" +
                                "${sample::class.java.canonicalName}$error"
                        )
                    }
                }

                try {
                    Thread.sleep(2000)
                    sample.runTestProcedures()
                    activity.runOnUiThread {
                        try {
                            fragmentManager.popBackStackImmediate()
                        } catch (_: Exception) {
                        }
                    }
                } catch (error: Exception) {
                    ok = false
                    error.printStackTrace()
                    Assert.fail(
                        "Error popping fragment ${sample.sampleTitle}" +
                            "${sample::class.java.canonicalName}$error"
                    )
                }

                Assert.assertTrue("the test failed", ok)
                System.gc()
                Log.i(
                    SamplesMenuFragment.TAG,
                    "Memory allocation: END Free: ${Runtime.getRuntime().freeMemory()} " +
                        "Total:${Runtime.getRuntime().totalMemory()} Max:${Runtime.getRuntime().maxMemory()}"
                )
            }
        }
    }

    companion object {
        private fun shuffleArray(array: IntArray) {
            val random = Random()
            for (index in array.lastIndex downTo 1) {
                val swapIndex = random.nextInt(index + 1)
                val value = array[swapIndex]
                array[swapIndex] = array[index]
                array[index] = value
            }
        }
    }
}

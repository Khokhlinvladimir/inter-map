/*
 * WARNING, All test cases exist in osmdroid-android-it/src/main/java (maven project)
 *
 * During build time (with gradle), these tests are copied from osmdroid-android-it to OpenStreetMapViewer/src/androidTest/java
 * DO NOT Modify files in OpenSteetMapViewer/src/androidTest. You will loose your changes when building!
 *
 */
package org.osmdroid.views

import android.graphics.Point
import android.view.View
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.osmdroid.R
import org.osmdroid.StarterMapActivity
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.util.TileSystemWebMercator
import java.util.Random
import kotlin.math.ln
import kotlin.math.max

class OpenStreetMapViewTest {
    @get:Rule
    val activityRule = ActivityTestRule(StarterMapActivity::class.java)

    private lateinit var mapView: MapView

    init {
        Counters.reset()
    }

    @Before
    fun setUp() {
        mapView = activityRule.activity
            .findViewById<View>(R.id.map_container)
            .findViewWithTag("mapView")
    }

    @Test
    fun test_toMapPixels_0_0() {
        activityRule.activity.runOnUiThread {
            val minimumZoom = ln(max(mapView.width, mapView.height) / TileSystem.tileSize.toDouble()) / ln(2.0)
            repeat(100) {
                checkCenter(getRandomZoom(minimumZoom), getRandomGeoPoint())
                checkCenter(getRandomZoom(minimumZoom), null)
                checkCenter(null, getRandomGeoPoint())
                checkCenter(null, null)
            }
        }
    }

    private fun getRandomLongitude() = tileSystem.getRandomLongitude(random.nextDouble())

    private fun getRandomLatitude() = tileSystem.getRandomLatitude(random.nextDouble(), tileSystem.minLatitude)

    private fun getRandomZoom(minimum: Double) = getRandom(minimum, TileSystem.maximumZoomLevel.toDouble())

    private fun getRandom(minimum: Double, maximum: Double) =
        minimum + random.nextDouble() * (maximum - minimum)

    private fun checkCenter(expectedZoom: Double?, expectedCenter: GeoPoint?) {
        expectedZoom?.let(mapView::setZoomLevel)
        expectedCenter?.let(mapView::setExpectedCenter)
        val projection = mapView.projection
        if (expectedZoom != null) {
            assertEquals("the zoom level is kept", expectedZoom, projection.zoomLevel, 0.0)
        }
        checkCenter(projection, mapView.mapCenter as GeoPoint, "computed")
        if (expectedCenter != null) {
            checkCenter(projection, expectedCenter, "assigned")
        }
    }

    private fun checkCenter(projection: Projection, center: GeoPoint, tag: String) {
        val roundingTolerance = 2.0
        val halfWidth = mapView.width / 2
        val halfHeight = mapView.height / 2
        val point = projection.toPixels(center, null)
        assertTrue(
            "MapView does not have layout. Make sure device is unlocked.",
            halfWidth > 0 && halfHeight > 0
        )
        val expected = Point(halfWidth, halfHeight)
        assertEquals(
            "the $tag center of the map is in the pixel center of the map (X)" +
                "(zoom=${projection.zoomLevel},center=$center)",
            expected.x.toDouble(),
            point.x.toDouble(),
            roundingTolerance
        )
        assertEquals(
            "the $tag center of the map is in the pixel center of the map (Y)" +
                "(zoom=${projection.zoomLevel},center=$center)",
            expected.y.toDouble(),
            point.y.toDouble(),
            roundingTolerance
        )
    }

    private fun getRandomGeoPoint() = GeoPoint(getRandomLatitude(), getRandomLongitude())

    companion object {
        private val random = Random()
        private val tileSystem: TileSystem = TileSystemWebMercator()
    }
}

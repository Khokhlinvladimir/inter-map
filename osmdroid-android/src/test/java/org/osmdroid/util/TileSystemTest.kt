package org.osmdroid.util

import android.graphics.Point
import org.junit.Assert
import org.junit.Test
import java.util.Random
import kotlin.math.abs

class TileSystemTest {
    @Test
    fun testGetY01FromLatitude() {
        checkXY01(0.0, tileSystem.getY01FromLatitude(tileSystem.maxLatitude, true))
        checkXY01(0.5, tileSystem.getY01FromLatitude(0.0, true))
        checkXY01(1.0, tileSystem.getY01FromLatitude(tileSystem.minLatitude, true))
    }

    @Test
    fun testGetX01FromLongitude() {
        val iterations = 10
        for (i in 0..iterations) {
            val longitude = tileSystem.minLongitude + i * (tileSystem.maxLongitude - tileSystem.minLongitude) / iterations
            checkXY01(i.toDouble() / iterations, tileSystem.getX01FromLongitude(longitude, true))
        }
    }

    @Test
    fun testGetLatitudeFromY01() {
        checkLatitude(tileSystem.maxLatitude, tileSystem.getLatitudeFromY01(0.0, true))
        checkLatitude(0.0, tileSystem.getLatitudeFromY01(0.5, true))
        checkLatitude(tileSystem.minLatitude, tileSystem.getLatitudeFromY01(1.0, true))
    }

    @Test
    fun testLatitude() = repeat(101) {
        val latitude = getRandomLatitude()
        checkLatitude(latitude, tileSystem.getLatitudeFromY01(tileSystem.getY01FromLatitude(latitude, true), true))
    }

    @Test
    fun testGetLongitudeFromX01() {
        val iterations = 10
        for (i in 0..iterations) {
            val longitude = tileSystem.minLongitude + i * (tileSystem.maxLongitude - tileSystem.minLongitude) / iterations
            checkLongitude(longitude, tileSystem.getLongitudeFromX01(i.toDouble() / iterations, true))
        }
        checkLongitude(tileSystem.minLongitude, tileSystem.getLongitudeFromX01(0.0, true))
        checkLongitude(0.0, tileSystem.getLongitudeFromX01(0.5, true))
        checkLongitude(tileSystem.maxLongitude, tileSystem.getLongitudeFromX01(1.0, true))
    }

    @Test
    fun testLongitude() = repeat(101) {
        val longitude = getRandomLongitude()
        checkLongitude(longitude, tileSystem.getLongitudeFromX01(tileSystem.getX01FromLongitude(longitude, true), true))
    }

    private fun checkXY01(expected: Double, actual: Double) {
        Assert.assertEquals(expected, actual, xy01Delta)
        checkMinMax(actual, 0.0, 1.0)
    }

    protected fun checkLatitude(expected: Double, actual: Double) {
        Assert.assertEquals(expected, actual, latLongDelta)
        checkMinMax(actual, tileSystem.minLatitude, tileSystem.maxLatitude)
    }

    protected fun checkLongitude(expected: Double, actual: Double) {
        Assert.assertEquals(expected, actual, latLongDelta)
        checkMinMax(actual, tileSystem.minLongitude, tileSystem.maxLongitude)
    }

    private fun checkMinMax(actual: Double, min: Double, max: Double) {
        Assert.assertTrue(actual <= max)
        Assert.assertTrue(actual >= min)
    }

    @Test
    fun testGetBoundingBoxZoom() {
        val tileSize = 256
        val screenWidth = tileSize * 2
        val screenHeight = screenWidth * 2
        TileSystem.tileSize = tileSize
        repeat(2000) {
            val north = getRandomLatitude(); val south = getRandomLatitude()
            val east = getRandomLongitude(); val west = getRandomLongitude()
            val boundingBox = BoundingBox(north, east, south, west)
            val zoom = tileSystem.getBoundingBoxZoom(boundingBox, screenWidth, screenHeight)
            if (zoom == Double.MIN_VALUE) {
                Assert.assertTrue(north <= south || east == west)
            } else {
                val mapSize = TileSystem.MapSize(zoom)
                val left = tileSystem.getMercatorXFromLongitude(west, mapSize, true)
                val top = tileSystem.getMercatorYFromLatitude(north, mapSize, true)
                val right = tileSystem.getMercatorXFromLongitude(east, mapSize, true)
                val bottom = tileSystem.getMercatorYFromLatitude(south, mapSize, true)
                var width = right - left
                if (east < west) width += mapSize.toLong()
                checkSize(width, bottom - top, screenWidth, screenHeight)
            }
        }
    }

    @Test
    fun test_MapSize() {
        for (zoomLevel in minZoomLevel..maxZoomLevel) {
            Assert.assertEquals(256L shl zoomLevel, TileSystem.MapSize(zoomLevel.toDouble()).toLong())
        }
    }

    @Test
    fun test_groundResolution() {
        val delta = 1e-4
        for (zoomLevel in minZoomLevel..maxZoomLevel) {
            Assert.assertEquals(156543.034 / (1 shl zoomLevel), TileSystem.GroundResolution(0.0, zoomLevel), delta)
        }
    }

    @Test
    fun test_groundMapScale() {
        val delta = 1e-2
        for (zoomLevel in minZoomLevel..maxZoomLevel) {
            Assert.assertEquals(591658710.9 / (1 shl zoomLevel), TileSystem.MapScale(0.0, zoomLevel, 96), delta)
        }
    }

    @Test
    fun test_LatLongToPixelXY() {
        val point = tileSystem.getMercatorFromGeo(60.0, 60.0, TileSystem.MapSize(10.0), null, true)
        Assert.assertEquals(174762, point.x)
        Assert.assertEquals(76126, point.y)
    }

    @Test
    fun test_PixelXYToLatLong() {
        val point = tileSystem.getGeoFromMercator(45, 45, TileSystem.MapSize(8.0), null, true, true)
        Assert.assertEquals(-179.752807617187, point.longitude, 1E-3)
        Assert.assertEquals(85.0297584051224, point.latitude, 1E-3)
    }

    @Test
    fun test_TileXYToQuadKey() {
        Assert.assertEquals("2", TileSystem.TileXYToQuadKey(0, 1, 1))
        Assert.assertEquals("13", TileSystem.TileXYToQuadKey(3, 1, 2))
        Assert.assertEquals("213", TileSystem.TileXYToQuadKey(3, 5, 3))
        var zero = ""; var one = ""; var two = ""; var three = ""
        for (zoom in 1..TileSystem.maximumZoomLevel) {
            zero += "0"; one += "1"; two += "2"; three += "3"
            val maxTile = (1 shl zoom) - 1
            Assert.assertEquals(zero, TileSystem.TileXYToQuadKey(0, 0, zoom))
            Assert.assertEquals(one, TileSystem.TileXYToQuadKey(maxTile, 0, zoom))
            Assert.assertEquals(two, TileSystem.TileXYToQuadKey(0, maxTile, zoom))
            Assert.assertEquals(three, TileSystem.TileXYToQuadKey(maxTile, maxTile, zoom))
        }
    }

    @Test
    fun test_QuadKeyToTileXY() {
        testPoint(0, 1, TileSystem.QuadKeyToTileXY("2", null))
        testPoint(3, 1, TileSystem.QuadKeyToTileXY("13", null))
        testPoint(3, 5, TileSystem.QuadKeyToTileXY("213", null))
        var zero = ""; var one = ""; var two = ""; var three = ""
        for (zoom in 1..TileSystem.maximumZoomLevel) {
            zero += "0"; one += "1"; two += "2"; three += "3"
            val maxTile = (1 shl zoom) - 1
            testPoint(0, 0, TileSystem.QuadKeyToTileXY(zero, null))
            testPoint(maxTile, 0, TileSystem.QuadKeyToTileXY(one, null))
            testPoint(0, maxTile, TileSystem.QuadKeyToTileXY(two, null))
            testPoint(maxTile, maxTile, TileSystem.QuadKeyToTileXY(three, null))
        }
    }

    private fun testPoint(expectedX: Int, expectedY: Int, actual: Point) {
        Assert.assertEquals(expectedX, actual.x); Assert.assertEquals(expectedY, actual.y)
    }

    private fun getRandomLongitude() = tileSystem.getRandomLongitude(random.nextDouble())
    private fun getRandomLatitude() = tileSystem.getRandomLatitude(random.nextDouble(), tileSystem.minLatitude)

    private fun checkSize(width: Long, height: Long, screenWidth: Int, screenHeight: Int) {
        val deltaWidth = abs(width - screenWidth)
        val deltaHeight = abs(height - screenHeight)
        if (deltaWidth <= deltaHeight) Assert.assertEquals(screenWidth.toDouble(), width.toDouble(), 2.0)
        else Assert.assertEquals(screenHeight.toDouble(), height.toDouble(), 2.0)
    }

    companion object {
        private val random = Random()
        private const val xy01Delta = 1E-10
        private const val latLongDelta = 1E-10
        private const val minZoomLevel = 0
        private val maxZoomLevel = TileSystem.maximumZoomLevel
        private val tileSystem: TileSystem = TileSystemWebMercator()
    }
}

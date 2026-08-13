package org.osmdroid.util

import android.graphics.Point
import android.graphics.Rect
import junit.framework.Assert
import org.junit.Test
import org.osmdroid.views.Projection
import java.util.Random
import kotlin.math.abs
import kotlin.math.min

class ProjectionTest {
    @Test
    fun testCenteredGeoPoint() {
        for (zoomLevel in minZoomLevel..maxZoomLevel) {
            val mapSize = TileSystem.MapSize(zoomLevel.toDouble())
            repeat(nbIterations) {
                val geoPoint = getRandomGeoPoint()
                val projection = getRandomProjection(zoomLevel.toDouble(), geoPoint, 0, 0)
                val pixel = projection.toPixels(geoPoint, null)
                var expectedX = width / 2
                if (mapSize < width) while (expectedX - mapSize >= 0) expectedX = (expectedX - mapSize).toInt()
                Assert.assertEquals(expectedX, pixel.x)
                var expectedY = height / 2
                if (mapSize < height) while (expectedY - mapSize >= 0) expectedY = (expectedY - mapSize).toInt()
                Assert.assertEquals(expectedY, pixel.y)
            }
        }
    }

    @Test
    fun testOffspringSameCenter() {
        val center = GeoPoint(0.0, 0.0)
        val pixel = Point()
        val centerX = (screenRect.right + screenRect.left) / 2
        val centerY = (screenRect.bottom + screenRect.top) / 2
        val miniCenterX = (miniMapScreenRect.right + miniMapScreenRect.left) / 2
        val miniCenterY = (miniMapScreenRect.bottom + miniMapScreenRect.top) / 2
        for (zoomLevel in minZoomLevel + minimapZoomLevelDifference..maxZoomLevel) {
            repeat(nbIterations) {
                val projection = getRandomProjection(zoomLevel.toDouble())
                val miniMapProjection = projection.getOffspring((zoomLevel - minimapZoomLevelDifference).toDouble(), miniMapScreenRect)
                projection.fromPixels(centerX, centerY, center)
                miniMapProjection.toPixels(center, pixel)
                Assert.assertEquals(miniCenterX, pixel.x)
                Assert.assertEquals(miniCenterY, pixel.y)
            }
        }
    }

    @Test
    fun testPixelToGeoToPixel() {
        val deltaPixel = 2
        for (zoomLevel in minZoomLevel..maxZoomLevel) {
            val mapSize = TileSystem.MapSize(zoomLevel.toDouble())
            repeat(nbIterations) {
                val pixelIn = getRandomPixel(mapSize)
                val projection = getRandomProjection(zoomLevel.toDouble())
                val geoPoint = projection.fromPixels(pixelIn.x, pixelIn.y)
                val pixelOut = projection.toPixels(geoPoint, null)
                if (mapSize < width) {
                    val diff = abs(pixelIn.x - pixelOut.x)
                    Assert.assertTrue(diff <= deltaPixel || abs(diff - mapSize) <= deltaPixel)
                } else Assert.assertEquals(pixelIn.x.toDouble(), pixelOut.x.toDouble(), deltaPixel.toDouble())
                if (mapSize < height) {
                    val diff = abs(pixelIn.y - pixelOut.y)
                    Assert.assertTrue(diff <= deltaPixel || abs(diff - mapSize) <= deltaPixel)
                } else Assert.assertEquals(pixelIn.y.toDouble(), pixelOut.y.toDouble(), deltaPixel.toDouble())
            }
        }
    }

    @Test
    fun testTilesOverlay() {
        val mercatorViewPort = RectL()
        val tiles = Rect()
        val displayedTile = Rect()
        repeat(nbIterations) {
            val zoomLevel = getRandomZoom()
            val tileSize = TileSystem.getTileSize(zoomLevel)
            val projection = getRandomProjection(zoomLevel)
            projection.getMercatorViewPort(mercatorViewPort)
            Assert.assertEquals(width.toLong(), mercatorViewPort.width())
            Assert.assertEquals(height.toLong(), mercatorViewPort.height())
            TileSystem.getTileFromMercator(mercatorViewPort, tileSize, tiles)
            Assert.assertTrue((tiles.right - tiles.left + 1) * tileSize >= width)
            Assert.assertTrue((tiles.bottom - tiles.top + 1) * tileSize >= height)
            var previousX = 0
            var previousY = 0
            for (i in tiles.left..tiles.right) {
                for (j in tiles.top..tiles.bottom) {
                    projection.getPixelFromTile(i, j, displayedTile)
                    if (j == tiles.bottom) Assert.assertTrue(displayedTile.bottom >= height)
                    if (j == tiles.top) Assert.assertTrue(displayedTile.top <= 0)
                    else Assert.assertTrue(displayedTile.top <= previousY + 1)
                    previousY = displayedTile.bottom
                }
                if (i == tiles.right) Assert.assertTrue(displayedTile.right >= width)
                if (i == tiles.left) Assert.assertTrue(displayedTile.left <= 0)
                else Assert.assertTrue(displayedTile.left <= previousX + 1)
                previousX = displayedTile.right
            }
        }
    }

    @Test
    fun testCheckScrollableOffset() {
        testCheckScrollableOffset(0, -100, 5000)
        testCheckScrollableOffset(0, 50, 950)
        testCheckScrollableOffset(-1, 52, 950)
        testCheckScrollableOffset(550, -100, 0)
        testCheckScrollableOffset(-650, 1100, 1200)
        testCheckScrollableOffset(-50, 100, 1950)
        testCheckScrollableOffset(450, -1000, 500)
    }

    private fun testCheckScrollableOffset(expected: Int, min: Int, max: Int) {
        Assert.assertEquals(expected.toLong(), Projection.getScrollableOffset(min.toLong(), max.toLong(), 20000.0, 1000, 50))
    }

    private fun getRandomPixel(mapSize: Double): Point {
        val pixel = Point()
        pixel.x = random.nextInt(min(width.toLong(), mapSize.toLong()).toInt())
        pixel.y = random.nextInt(min(height.toLong(), mapSize.toLong()).toInt())
        return pixel
    }

    private fun getRandomOffset(mapSize: Double): Long {
        var result = (random.nextDouble() * mapSize).toLong()
        result -= result / 2
        return result
    }

    private fun getRandomGeoPoint() = GeoPoint(getRandomLatitude(), getRandomLongitude())
    private fun getRandomLongitude() = tileSystem.getRandomLongitude(random.nextDouble())
    private fun getRandomLatitude() = tileSystem.getRandomLatitude(random.nextDouble(), tileSystem.minLatitude)
    private fun getRandom(min: Double, max: Double) = min + random.nextDouble() * (max - min)
    private fun getRandomZoom() = getRandom(minZoomLevel.toDouble(), maxZoomLevel.toDouble())
    private fun getRandomOrientation() = getRandom(-180.0, 180.0).toFloat()

    private fun getRandomProjection(zoomLevel: Double, geoPoint: GeoPoint, offsetX: Long, offsetY: Long) =
        Projection(zoomLevel, screenRect, geoPoint, offsetX, offsetY, getRandomOrientation(), true, true, tileSystem, 0, 0)

    private fun getRandomProjection(zoomLevel: Double): Projection {
        val mapSize = TileSystem.MapSize(zoomLevel)
        return getRandomProjection(zoomLevel, getRandomGeoPoint(), getRandomOffset(mapSize), getRandomOffset(mapSize))
    }

    @Test
    fun test_conversionFromPixelsToPixels() {
        for (zoomLevel in minZoomLevel..maxZoomLevel) {
            val projection = Projection(
                zoomLevel.toDouble(), Rect(0, 0, 1080, 1536), GeoPoint(0.0, 0.0),
                0L, 0L, 0f, false, false, tileSystem, 0, 0
            )
            val inputPoint = Point(0, 0)
            val geoPoint = projection.fromPixels(inputPoint.x, inputPoint.y) as GeoPoint
            val outputPoint = projection.toPixels(geoPoint, null)
            Assert.assertEquals(inputPoint.x, outputPoint.x)
            Assert.assertEquals(inputPoint.y, outputPoint.y)
        }
    }

    companion object {
        private val random = Random()
        private const val minZoomLevel = 0
        private val maxZoomLevel = TileSystem.maximumZoomLevel
        private const val minimapZoomLevelDifference = 5
        private const val nbIterations = 1000
        private const val width = 600
        private const val height = 800
        private val screenRect = Rect().apply { left = 0; top = 0; right = width; bottom = height }
        private val miniMapScreenRect = Rect().apply {
            left = width / 2; top = height / 2; right = left + width / 4; bottom = top + height / 4
        }
        private val tileSystem: TileSystem = TileSystemWebMercator()
    }
}

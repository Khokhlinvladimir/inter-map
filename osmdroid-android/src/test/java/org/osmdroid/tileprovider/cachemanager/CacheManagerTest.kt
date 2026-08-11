package org.osmdroid.tileprovider.cachemanager

import android.graphics.Rect
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.MyMath
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Random

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CacheManagerTest {
    private val mRandom = Random()

    @Test
    fun testGetTilesIterableForBigMapTileUpperBound() = verifyGetTilesIterable(15, 15)

    @Test
    fun testGetTilesIterableForSmallMapTileUpperBound() = verifyGetTilesIterable(2, 2)

    @Test
    fun testGetTilesIterableForRangeOfZooms() = verifyGetTilesIterable(10, 11)

    private fun verifyGetTilesIterable(pMinZoom: Int, pMaxZoom: Int) {
        val boundingBox = BoundingBox(
            52.95131467958858, 13.6473953271975,
            52.886830733534954, 13.3473953271975
        )
        val allPointsList = ArrayList<Long?>()
        for (zoomLevel in pMinZoom..pMaxZoom) {
            allPointsList.addAll(getTilesCoverage(boundingBox, zoomLevel))
        }
        val allPointsIterator = allPointsList.iterator()
        val iterableWithSize = CacheManager.getTilesCoverageIterable(boundingBox, pMinZoom, pMaxZoom)

        Assert.assertEquals(allPointsList.size, iterableWithSize.size())
        for (value in CacheManager.getTilesCoverageIterable(boundingBox, pMinZoom, pMaxZoom)) {
            Assert.assertEquals(allPointsIterator.next(), value)
        }
    }

    private fun getTilesCoverage(pBB: BoundingBox, pZoomLevel: Int): Collection<Long?> {
        val result = LinkedHashSet<Long?>()
        val mapTileUpperBound = 1 shl pZoomLevel
        val rect: Rect = CacheManager.getTilesRect(pBB, pZoomLevel)
        for (j in rect.top..rect.bottom) {
            for (i in rect.left..rect.right) {
                val x = MyMath.mod(i, mapTileUpperBound)
                val y = MyMath.mod(j, mapTileUpperBound)
                result.add(MapTileIndex.getTileIndex(pZoomLevel, x, y))
            }
        }
        return result
    }

    @Test
    fun testGetTilesRectSingleTile() {
        val tileSystem = getTileSystem()
        val box = BoundingBox()
        for (zoom in 0..TileSystem.maximumZoomLevel) {
            val longitude = tileSystem.getRandomLongitude(mRandom.nextDouble())
            val latitude = tileSystem.getRandomLatitude(mRandom.nextDouble())
            box.set(latitude, longitude, latitude, longitude)
            val rect = CacheManager.getTilesRect(box, zoom)
            Assert.assertEquals(rect.left, rect.right)
            Assert.assertEquals(rect.top, rect.bottom)
        }
    }

    @Test
    fun testGetTilesRectWholeWorld() {
        val tileSystem = getTileSystem()
        val box = BoundingBox(
            tileSystem.maxLatitude, tileSystem.maxLongitude,
            tileSystem.minLatitude, tileSystem.minLongitude
        )
        for (zoom in 0..TileSystem.maximumZoomLevel) {
            val rect = CacheManager.getTilesRect(box, zoom)
            Assert.assertEquals(0, rect.left)
            Assert.assertEquals(0, rect.top)
            val maxSize = -1 + (1 shl zoom)
            Assert.assertEquals(maxSize, rect.bottom)
            Assert.assertEquals(maxSize, rect.right)
        }
    }
}

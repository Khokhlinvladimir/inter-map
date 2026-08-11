package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test
import java.util.Random

class MapTileListTest {
    @Test
    fun testGetPut() {
        val iterations = 100
        val maxSize = 20
        repeat(iterations) {
            val size = 1 + random.nextInt(maxSize)
            val array = LongArray(size) { random.nextLong() }
            val list = MapTileList()
            array.forEach(list::put)
            check(array, list)
            list.clear()
            check(LongArray(0), list)
        }
    }

    private fun check(array: LongArray, list: MapTileList) {
        Assert.assertEquals(array.size, list.size)
        for (i in array.indices) Assert.assertEquals(array[i], list.get(i))
    }

    @Test
    fun testPutBoundingBox() {
        val iterations = 100
        val zoom = 4
        val max = 1 shl zoom
        val list = MapTileList()
        repeat(iterations) {
            val left = random.nextInt(max)
            val top = random.nextInt(max)
            val right = random.nextInt(max)
            val bottom = random.nextInt(max)
            list.clear()
            list.put(zoom, left, top, right, bottom)
            val spanX = right - left + 1 + if (right < left) max else 0
            val spanY = bottom - top + 1 + if (bottom < top) max else 0
            Assert.assertEquals(spanX * spanY, list.size)
            Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, left, top)))
            Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, left, bottom)))
            Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, right, top)))
            Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, right, bottom)))
            for (j in 0 until list.size) Assert.assertEquals(zoom, MapTileIndex.getZoom(list.get(j)))
        }
    }

    @Test
    fun testPutZoom() {
        val maxZoom = 3
        val left = 0
        val top = 0
        val list = MapTileList()
        for (zoom in 0..maxZoom) {
            val max = 1 shl zoom
            val right = max - 1
            val bottom = max - 1
            list.clear()
            list.put(zoom)
            val spanX = right - left + 1 + if (right < left) max else 0
            val spanY = bottom - top + 1 + if (bottom < top) max else 0
            Assert.assertEquals(spanX * spanY, list.size)
            Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, left, top)))
            Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, left, bottom)))
            Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, right, top)))
            Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, right, bottom)))
            for (j in 0 until list.size) Assert.assertEquals(zoom, MapTileIndex.getZoom(list.get(j)))
        }
    }

    @Test
    fun testEmpty() {
        val list = MapTileList()
        Assert.assertEquals(0, list.size)
        Assert.assertFalse(list.contains(1234))
    }

    companion object {
        private val random = Random()
    }
}

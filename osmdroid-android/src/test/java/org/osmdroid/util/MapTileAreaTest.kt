package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test
import java.util.Random

class MapTileAreaTest {
    private val random = Random()

    @Test
    fun testSetAll() {
        val area = MapTileArea()
        for (zoom in 0..2) {
            area.set(zoom, -10, -100, 50, 90)
            checkAll(zoom, area)
        }
    }

    @Test
    fun testSize() {
        val area = MapTileArea()
        for (zoom in 0..TileSystem.maximumZoomLevel) {
            val upperBound = 1 shl zoom
            val size = upperBound.toLong() * upperBound
            if (size >= Int.MAX_VALUE) return
            setNewWorld(area, zoom)
            Assert.assertEquals(size, area.size().toLong())
            Assert.assertTrue(area.iterator().hasNext())
        }
    }

    @Test
    fun testCorners() {
        val area = MapTileArea()
        for (zoom in 0..TileSystem.maximumZoomLevel) {
            val max = (1 shl zoom) - 1
            setNewWorld(area, zoom)
            Assert.assertTrue(area.contains(MapTileIndex.getTileIndex(zoom, 0, 0)))
            Assert.assertTrue(area.contains(MapTileIndex.getTileIndex(zoom, 0, max)))
            Assert.assertTrue(area.contains(MapTileIndex.getTileIndex(zoom, max, max)))
            Assert.assertTrue(area.contains(MapTileIndex.getTileIndex(zoom, max, 0)))
        }
    }

    @Test
    fun testNextSize() {
        val set = HashSet<Long>()
        val area = MapTileArea()
        for (zoom in 0..TileSystem.maximumZoomLevel) {
            val upperBound = 1 shl zoom
            val size = upperBound.toLong() * upperBound
            if (size >= Int.MAX_VALUE || size >= 1000) return
            setNewWorld(area, zoom)
            Assert.assertEquals(size, area.size().toLong())
            var count = 0
            set.clear()
            for (nullableIndex in area) {
                val index = nullableIndex!!
                count++
                Assert.assertEquals(zoom, MapTileIndex.getZoom(index))
                val x = MapTileIndex.getX(index)
                val y = MapTileIndex.getY(index)
                Assert.assertTrue(x in 0 until upperBound)
                Assert.assertTrue(y in 0 until upperBound)
                set.add(index)
            }
            Assert.assertEquals(size, set.size.toLong())
            Assert.assertEquals(size, count.toLong())
        }
    }

    @Test
    fun testPerformances() {
        val list = MapTileList()
        val area = MapTileArea()
        val zoom = 10
        val size = 10
        list.ensureCapacity(size * size)
        var start = System.nanoTime()
        list.put(zoom, 0, 0, size - 1, size - 1)
        var duration1 = System.nanoTime() - start
        start = System.nanoTime()
        area.set(zoom, 0, 0, size - 1, size - 1)
        var duration2 = System.nanoTime() - start
        checkDuration(duration1, duration2)
        checkContainDuration(zoom, zoom, size - 1, size - 1, list, area, true)
        checkContainDuration(0, zoom - 1, 0, 0, list, area, false)
        checkContainDuration(zoom + 1, TileSystem.maximumZoomLevel, 0, 0, list, area, false)
    }

    private fun checkContainDuration(
        zoomMin: Int,
        zoomMax: Int,
        xMax: Int,
        yMax: Int,
        list: MapTileList,
        area: MapTileArea,
        expected: Boolean
    ) {
        var duration1 = 0L
        var duration2 = 0L
        for (zoom in zoomMin..zoomMax) {
            for (x in 0..xMax) {
                for (y in 0..yMax) {
                    val index = MapTileIndex.getTileIndex(zoom, x, y)
                    var start = System.nanoTime()
                    Assert.assertEquals(expected, list.contains(index))
                    duration1 += System.nanoTime() - start
                    start = System.nanoTime()
                    Assert.assertEquals(expected, area.contains(index))
                    duration2 += System.nanoTime() - start
                }
            }
        }
        checkDuration(duration1, duration2)
    }

    private fun checkDuration(duration1: Long, duration2: Long) {
        System.err.println("$duration2 < $duration1")
    }

    private fun setNewWorld(area: MapTileArea, zoom: Int) {
        val upperBound = 1 shl zoom
        val left = random.nextInt(upperBound)
        val top = random.nextInt(upperBound)
        area.set(
            zoom,
            left,
            top,
            (left + upperBound - 1) % upperBound,
            (top + upperBound - 1) % upperBound
        )
    }

    private fun checkAll(zoom: Int, area: MapTileArea) {
        val max = 1 shl zoom
        Assert.assertEquals(max * max, area.size())
        for (x in 0 until max) for (y in 0 until max) {
            Assert.assertTrue(area.contains(MapTileIndex.getTileIndex(zoom, x, y)))
        }
    }
}

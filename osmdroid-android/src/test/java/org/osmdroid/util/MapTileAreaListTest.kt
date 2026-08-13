package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test
import java.util.Random

class MapTileAreaListTest {
    private val random = Random()

    @Test
    fun test() {
        val decentMax = 10
        val list = MapTileAreaList()
        Assert.assertEquals(0, list.size())
        val counts = ArrayList<Int>()
        var count = 0
        for (zoom in 0..TileSystem.maximumZoomLevel) {
            val number = random.nextInt(1 shl zoom) % decentMax
            val size = (number + 1) * (number + 1)
            counts.add(size)
            count += size
            list.list.add(MapTileArea().set(zoom, 0, 0, number, number))
            Assert.assertEquals(count, list.size())
            for (x in 0..number) for (y in 0..number) {
                Assert.assertTrue(list.contains(MapTileIndex.getTileIndex(zoom, x, y)))
            }
        }
        var zoom = -1
        count = 0
        var total = 0
        for (nullableIndex in list) {
            val index = nullableIndex!!
            total++
            val newZoom = MapTileIndex.getZoom(index)
            if (zoom != newZoom) {
                if (zoom != -1) Assert.assertEquals(counts[zoom], count)
                count = 0
                zoom = newZoom
            }
            count++
        }
        Assert.assertEquals(counts[zoom], count)
        Assert.assertEquals(list.size(), total)
    }
}

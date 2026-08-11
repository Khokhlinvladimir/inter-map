package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

class MapTileAreaBorderComputerTest {
    @Test
    fun testOnePointModulo() {
        val source = MapTileArea()
        val dest = MapTileArea()
        val set = HashSet<Long>()
        val border = 2
        val zoom = 5
        val sourceX = 1
        val sourceY = 31
        source.set(zoom, sourceX, sourceY, sourceX, sourceY)
        add(set, zoom, sourceX, sourceY, border)
        MapTileAreaBorderComputer(border).computeFromSource(source, dest)
        check(dest, set, zoom)
    }

    @Test
    fun testTwoContiguousPointsModulo() {
        val source = MapTileArea()
        val dest = MapTileArea()
        val set = HashSet<Long>()
        val border = 2
        val zoom = 5
        val sourceX = 1
        val sourceY = 31
        source.set(zoom, sourceX, sourceY, sourceX + 1, sourceY)
        add(set, zoom, sourceX, sourceY, border)
        add(set, zoom, sourceX + 1, sourceY, border)
        MapTileAreaBorderComputer(border).computeFromSource(source, dest)
        check(dest, set, zoom)
    }

    private fun check(area: MapTileArea, set: Set<Long>, zoom: Int) {
        val unique = HashSet<Long>()
        for (index in area) {
            Assert.assertNotNull(index)
            Assert.assertTrue(unique.add(index!!))
            Assert.assertEquals(zoom, MapTileIndex.getZoom(index))
            Assert.assertTrue(set.contains(index))
        }
        Assert.assertEquals(set.size, area.size())
    }

    private fun add(set: MutableSet<Long>, zoom: Int, x: Int, y: Int, border: Int) {
        val power = 1 shl zoom
        for (i in x - border..x + border) {
            for (j in y - border..y + border) {
                set.add(MapTileIndex.getTileIndex(zoom, (i + power) % power, (j + power) % power))
            }
        }
    }
}

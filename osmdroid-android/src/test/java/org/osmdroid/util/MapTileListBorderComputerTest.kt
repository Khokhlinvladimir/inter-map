package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

/** Unit tests related to [MapTileListBorderComputer]. */
@Deprecated("Use MapTileAreaBorderComputerTest instead")
class MapTileListBorderComputerTest {
    @Test
    fun testOnePointModuloInclude() {
        val source = MapTileList()
        val dest = MapTileList()
        val set = HashSet<Long>()
        val border = 2
        val computer = MapTileListBorderComputer(border, true)
        val zoom = 5
        val sourceX = 1
        val sourceY = 31
        source.put(MapTileIndex.getTileIndex(zoom, sourceX, sourceY))
        add(set, zoom, sourceX, sourceY, border)
        computer.computeFromSource(source, dest)
        check(dest, set, zoom)
    }

    @Test
    fun testOnePointModulo() {
        val source = MapTileList()
        val dest = MapTileList()
        val set = HashSet<Long>()
        val border = 2
        val computer = MapTileListBorderComputer(border, false)
        val zoom = 5
        val sourceX = 1
        val sourceY = 31
        source.put(MapTileIndex.getTileIndex(zoom, sourceX, sourceY))
        add(set, zoom, sourceX, sourceY, border)
        set.remove(MapTileIndex.getTileIndex(zoom, sourceX, sourceY))
        computer.computeFromSource(source, dest)
        check(dest, set, zoom)
    }

    @Test
    fun testTwoContiguousPointsModuloInclude() {
        val source = MapTileList()
        val dest = MapTileList()
        val set = HashSet<Long>()
        val border = 2
        val computer = MapTileListBorderComputer(border, true)
        val zoom = 5
        val sourceX = 1
        val sourceY = 31
        source.put(MapTileIndex.getTileIndex(zoom, sourceX, sourceY))
        source.put(MapTileIndex.getTileIndex(zoom, sourceX + 1, sourceY))
        add(set, zoom, sourceX, sourceY, border)
        add(set, zoom, sourceX + 1, sourceY, border)
        computer.computeFromSource(source, dest)
        check(dest, set, zoom)
    }

    private fun check(mapTileList: MapTileList, set: Set<Long>, zoom: Int) {
        checkUnique(mapTileList)
        checkZoom(mapTileList, zoom)
        checkEquals(mapTileList, set)
    }

    private fun checkEquals(mapTileList: MapTileList, set: Set<Long>) {
        Assert.assertEquals(set.size, mapTileList.size)
        for (i in 0 until mapTileList.size) Assert.assertTrue(set.contains(mapTileList.get(i)))
    }

    private fun checkUnique(mapTileList: MapTileList) {
        val set = HashSet<Long>()
        for (i in 0 until mapTileList.size) Assert.assertTrue(set.add(mapTileList.get(i)))
    }

    private fun checkZoom(mapTileList: MapTileList, zoom: Int) {
        for (i in 0 until mapTileList.size) Assert.assertEquals(zoom, MapTileIndex.getZoom(mapTileList.get(i)))
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

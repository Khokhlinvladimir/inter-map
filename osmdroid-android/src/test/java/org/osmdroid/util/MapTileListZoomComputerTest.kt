package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

@Deprecated("Use MapTileAreaZoomComputerTest instead")
class MapTileListZoomComputerTest {
    @Test
    fun testComputeFromSource() {
        val source = MapTileList()
        val dest = MapTileList()
        val set = HashSet<Long>()
        val sourceZoom = 5
        val sourceXMin = 10
        val sourceXMax = 15
        val sourceYMin = 20
        val sourceYMax = 22
        val destMinus1XMin = sourceXMin shr 1
        val destMinus1XMax = sourceXMax shr 1
        val destMinus1YMin = sourceYMin shr 1
        val destMinus1YMax = sourceYMax shr 1
        val destPlus1XMin = sourceXMin shl 1
        val destPlus1XMax = (sourceXMax shl 1) + 1
        val destPlus1YMin = sourceYMin shl 1
        val destPlus1YMax = (sourceYMax shl 1) + 1
        for (i in sourceXMin..sourceXMax) for (j in sourceYMin..sourceYMax) {
            source.put(MapTileIndex.getTileIndex(sourceZoom, i, j))
        }
        Assert.assertEquals((sourceXMax - sourceXMin + 1) * (sourceYMax - sourceYMin + 1), source.size)

        val minMaxDelta = 4
        for (zoomDelta in -minMaxDelta until minMaxDelta) {
            val computer = MapTileListZoomComputer(zoomDelta)
            dest.clear()
            computer.computeFromSource(source, dest)
            val tag = "zoomDelta=$zoomDelta"
            when {
                sourceZoom + zoomDelta < 0 || sourceZoom + zoomDelta > MapTileIndex.mMaxZoomLevel -> Assert.assertEquals(tag, 0, dest.size)
                zoomDelta <= 0 -> Assert.assertEquals(tag, source.size, dest.size)
                else -> Assert.assertEquals(tag, source.size shl (2 * zoomDelta), dest.size)
            }
        }

        var computer = MapTileListZoomComputer(-1)
        dest.clear()
        computer.computeFromSource(source, dest)
        set.clear()
        populateSet(set, dest)
        check(set, sourceZoom + computer.zoomDelta, destMinus1XMin, destMinus1XMax, destMinus1YMin, destMinus1YMax)

        computer = MapTileListZoomComputer(1)
        dest.clear()
        computer.computeFromSource(source, dest)
        set.clear()
        populateSet(set, dest)
        check(set, sourceZoom + computer.zoomDelta, destPlus1XMin, destPlus1XMax, destPlus1YMin, destPlus1YMax)
    }

    private fun check(set: Set<Long>, zoom: Int, xMin: Int, xMax: Int, yMin: Int, yMax: Int) {
        Assert.assertEquals((xMax - xMin + 1) * (yMax - yMin + 1), set.size)
        for (expectedX in xMin..xMax) {
            // Preserve the original test's single-value Y loop.
            for (expectedY in yMax..yMax) Assert.assertTrue(set.contains(MapTileIndex.getTileIndex(zoom, expectedX, expectedY)))
        }
    }

    private fun populateSet(set: MutableSet<Long>, list: MapTileList) {
        for (i in 0 until list.size) set.add(list.get(i))
    }
}

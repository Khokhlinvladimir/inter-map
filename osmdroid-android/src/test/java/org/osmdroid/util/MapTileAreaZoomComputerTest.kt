package org.osmdroid.util

import android.graphics.Rect
import junit.framework.Assert
import org.junit.Test

class MapTileAreaZoomComputerTest {
    @Test
    fun testWorld() {
        val src = MapTileArea()
        val dst = MapTileArea()
        for (zoom in 0..TileSystem.maximumZoomLevel) {
            var upperBound = 1 shl zoom
            var size = upperBound.toLong() * upperBound
            if (size >= Int.MAX_VALUE) return
            for (i in 0..1) {
                val rect = Rect()
                if (i == 0) {
                    rect.left = 0
                    rect.top = 0
                    rect.right = upperBound - 1
                    rect.bottom = upperBound - 1
                } else {
                    if (zoom == 0) continue
                    rect.left = 0
                    rect.top = 0
                    rect.right = upperBound / 2 - 1
                    rect.bottom = upperBound / 2 - 1
                }
                src.set(zoom, rect)
                val srcSize = src.size().toLong()
                for (zoomDelta in 0..TileSystem.maximumZoomLevel) {
                    val newZoom = zoom + zoomDelta
                    if (newZoom !in 0..TileSystem.maximumZoomLevel) continue
                    upperBound = 1 shl newZoom
                    size = upperBound.toLong() * upperBound
                    if (size >= Int.MAX_VALUE) return
                    MapTileAreaZoomComputer(zoomDelta).computeFromSource(src, dst)
                    val dstSize = dst.size().toLong()
                    val message = "zoom=$zoom, delta=$zoomDelta"
                    if (zoomDelta == 0) {
                        Assert.assertEquals(message, srcSize, dstSize)
                    } else if (zoomDelta < 0) {
                        Assert.assertEquals(message, srcSize * (1 shr -zoomDelta) * (1 shr -zoomDelta), dstSize)
                    } else {
                        Assert.assertEquals(message, srcSize * (1 shl zoomDelta) * (1 shl zoomDelta), dstSize)
                    }
                }
            }
        }
    }

    @Test
    fun testBugANRSideEffect() {
        val source = MapTileArea().set(0, 0, 0, 1, 1)
        val dest = MapTileArea()
        MapTileAreaZoomComputer(-1).computeFromSource(source, dest)
        Assert.assertEquals(0, dest.width)
    }
}

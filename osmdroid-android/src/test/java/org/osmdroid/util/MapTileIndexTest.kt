package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test
import java.util.Random

class MapTileIndexTest {
    @Test
    fun testIndex() {
        repeat(1000) {
            val zoom = random.nextInt(TileSystem.primaryKeyMaxZoomLevel + 1)
            val x = random.nextInt(1 shl zoom)
            val y = random.nextInt(1 shl zoom)
            val index = MapTileIndex.getTileIndex(zoom, x, y)
            Assert.assertEquals(zoom, MapTileIndex.getZoom(index))
            Assert.assertEquals(x, MapTileIndex.getX(index))
            Assert.assertEquals(y, MapTileIndex.getY(index))
        }
    }

    companion object {
        private val random = Random()
    }
}

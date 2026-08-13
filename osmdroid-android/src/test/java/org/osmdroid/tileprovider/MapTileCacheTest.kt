package org.osmdroid.tileprovider

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import junit.framework.Assert
import org.junit.Test
import org.osmdroid.util.MapTileIndex

/** Unit tests related to [MapTileCache]. */
class MapTileCacheTest {
    private val mZoom = 10

    @Test
    fun testCapacity() {
        val drawable = getNonNullDrawable()
        val capacity = 50
        val extra = 4
        val extraExtra = 3
        val mapTileCache = MapTileCache(capacity)
        val mapTileArea = mapTileCache.mapTileArea

        Assert.assertEquals(0, mapTileCache.size)

        for (i in 0 until capacity + extra) {
            mapTileCache.putTile(getMapTileIndex(i), drawable)
        }
        Assert.assertEquals(capacity + extra, mapTileCache.size)

        for (i in 0 until capacity + extra) {
            mapTileCache.putTile(getMapTileIndex(i), drawable)
        }
        Assert.assertEquals(capacity + extra, mapTileCache.size)

        mapTileArea.set(mZoom, 0, 0, 0, capacity + extra + extraExtra - 1)
        mapTileCache.garbageCollection()
        Assert.assertEquals(capacity + extra, mapTileCache.size)

        mapTileArea.set(mZoom, 0, 0, 0, capacity + extra - 1)
        mapTileCache.garbageCollection()
        Assert.assertEquals(capacity + extra, mapTileCache.size)

        mapTileArea.set(mZoom, 0, 0, 0, capacity - 1)
        mapTileCache.garbageCollection()
        Assert.assertEquals(capacity, mapTileCache.size)
        for (i in 0 until capacity + extra) {
            val value = mapTileCache.getMapTile(getMapTileIndex(i))
            if (i < capacity) Assert.assertNotNull(value) else Assert.assertNull(value)
        }

        mapTileArea.reset()
        mapTileCache.garbageCollection()
        Assert.assertEquals(capacity, mapTileCache.size)

        mapTileCache.clear()
        Assert.assertEquals(0, mapTileCache.size)
    }

    private fun getMapTileIndex(pIndex: Int): Long = MapTileIndex.getTileIndex(mZoom, 0, pIndex)

    private fun getNonNullDrawable(): Drawable = object : Drawable() {
        override fun draw(canvas: Canvas) = Unit
        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        override fun getOpacity(): Int = 0
    }

    @Test
    @Throws(InterruptedException::class)
    fun testConcurrency() {
        val mapTileCache = MapTileCache(100)
        val threads = ArrayList<Thread>()
        val dummyDrawable: Drawable = ColorDrawable(0x0)
        val numTiles = 10000

        for (i in 0 until numTiles) mapTileCache.putTile(i.toLong(), dummyDrawable)

        for (i in 0 until 10) {
            val thread = object : Thread() {
                override fun run() {
                    for (j in 0 until numTiles) mapTileCache.remove(j.toLong())
                }
            }
            thread.start()
            threads.add(thread)
        }

        for (thread in threads) thread.join()

        mapTileCache.clear()
        Assert.assertEquals(0, mapTileCache.size)
    }
}

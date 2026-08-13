/*
 * WARNING, All test cases exist in osmdroid-android-it/src/main/java (maven project)
 *
 * During build time (with gradle), these tests are copied from osmdroid-android-it to OpenStreetMapViewer/src/androidTest/java
 * DO NOT Modify files in OpenSteetMapViewer/src/androidTest. You will loose your changes when building!
 *
 */
package org.osmdroid.tileprovider.modules

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.tileprovider.IMapTileProviderCallback
import org.osmdroid.tileprovider.MapTileRequestState
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import java.util.ArrayList
import java.util.LinkedList

class MapTileProviderTest {
    private val tiles: MutableList<Long> = LinkedList()
    private val providers: MutableList<MapTileModuleProviderBase?> = ArrayList()

    private val tileProviderCallback = object : IMapTileProviderCallback {
        override fun mapTileRequestCompleted(aState: MapTileRequestState?, aDrawable: Drawable?) {
            tiles.add(aState!!.mapTile)
        }

        override fun mapTileRequestFailed(aState: MapTileRequestState?) = Unit

        override fun mapTileRequestFailedExceedsMaxQueueSize(aState: MapTileRequestState?) = Unit

        override fun mapTileRequestExpiredTile(aState: MapTileRequestState?, aDrawable: Drawable?) = Unit

        override fun useDataConnection() = false
    }

    private val tileProvider = object : MapTileModuleProviderBase(1, 10) {
        override fun getThreadGroupName() = "OpenStreetMapAsyncTileProviderTest"

        override fun getTileLoader(): TileLoader = object : TileLoader() {
            override fun loadTile(pMapTileIndex: Long): Drawable {
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                }
                return BitmapDrawable()
            }
        }

        override fun getUsesDataConnection() = false

        override fun getMinimumZoomLevel() = 0

        override fun getMaximumZoomLevel() = 10

        override fun getName() = "test"

        override fun setTileSource(tileSource: ITileSource?) = Unit
    }

    @Test
    fun test_put_twice() {
        val tile = MapTileIndex.getTileIndex(1, 1, 1)
        val state = MapTileRequestState(tile, providers, tileProviderCallback)
        tileProvider.loadMapTileAsync(state)
        tileProvider.loadMapTileAsync(state)
        assertEquals("One tile pending", 1, tileProvider.mPending.size)
    }

    @Test
    fun test_order() {
        val tile1 = MapTileIndex.getTileIndex(1, 1, 1)
        val tile2 = MapTileIndex.getTileIndex(2, 2, 2)
        val tile3 = MapTileIndex.getTileIndex(3, 3, 3)

        tileProvider.loadMapTileAsync(MapTileRequestState(tile1, providers, tileProviderCallback))
        Thread.sleep(100)
        tileProvider.loadMapTileAsync(MapTileRequestState(tile2, providers, tileProviderCallback))
        Thread.sleep(100)
        tileProvider.loadMapTileAsync(MapTileRequestState(tile3, providers, tileProviderCallback))

        val timeout = System.currentTimeMillis() + 10000
        while (tiles.size != 3 && System.currentTimeMillis() < timeout) {
            Thread.sleep(250)
        }

        assertEquals("Three tiles in the list", 3, tiles.size)
        assertEquals("tile1 is first", tile1, tiles[0])
        assertEquals("tile3 is second", tile3, tiles[1])
        assertEquals("tile2 is third", tile2, tiles[2])
    }

    @Test
    fun test_jump_queue() {
        val tile1 = MapTileIndex.getTileIndex(1, 1, 1)
        val tile2 = MapTileIndex.getTileIndex(2, 2, 2)
        val tile3 = MapTileIndex.getTileIndex(3, 3, 3)

        tileProvider.loadMapTileAsync(MapTileRequestState(tile1, providers, tileProviderCallback))
        Thread.sleep(100)
        tileProvider.loadMapTileAsync(MapTileRequestState(tile2, providers, tileProviderCallback))
        Thread.sleep(100)
        tileProvider.loadMapTileAsync(MapTileRequestState(tile3, providers, tileProviderCallback))
        Thread.sleep(100)
        tileProvider.loadMapTileAsync(MapTileRequestState(tile2, providers, tileProviderCallback))

        val timeout = System.currentTimeMillis() + 10000
        while (tiles.size != 3 && System.currentTimeMillis() < timeout) {
            Thread.sleep(250)
        }

        assertEquals("Three tiles in the list", 3, tiles.size)
        assertEquals("tile1 is first", tile1, tiles[0])
        assertEquals("tile2 is second", tile2, tiles[1])
        assertEquals("tile3 is third", tile3, tiles[2])
    }
}

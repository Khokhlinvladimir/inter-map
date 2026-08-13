package org.osmdroid.tileprovider

import org.osmdroid.tileprovider.modules.CantContinueException
import org.osmdroid.tileprovider.modules.MapTileDownloader
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GarbageCollector
import org.osmdroid.util.MapTileArea
import org.osmdroid.util.MapTileAreaList

/**
 * The Tile Pre Cache goal is to:
 * - list the tiles that are near the ones that are currently displayed (border, zoom+-1, ...)
 * - try to find their bitmap using a list of providers
 * - pre-cache those bitmaps into memory cache
 * Doing so you get smoother when panning the map or zooming in/out
 * as the bitmaps are already in memory.
 * Cf. [#930](https://github.com/osmdroid/osmdroid/issues/930)
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
class MapTilePreCache(pCache: MapTileCache) {
    private val mProviders: MutableList<MapTileModuleProviderBase> = ArrayList<MapTileModuleProviderBase>() // cf. MapTileApproximater
    private val mTileAreas = MapTileAreaList()
    private var mTileIndices: MutableIterator<Long?>? = null
    private val mCache: MapTileCache
    private val mGC = GarbageCollector(object : Runnable {
        override fun run() {
            var next: Long
            while ((next().also { next = it }) != -1L) {
                search(next)
            }
        }
    })

    init {
        mCache = pCache
    }

    fun addProvider(pProvider: MapTileModuleProviderBase?) {
        mProviders.add(pProvider!!)
    }

    /**
     * Compute the latest tile list and try to put each tile bitmap in memory cache
     */
    fun fill() {
        if (mGC.isRunning) {
            return
        }
        refresh()
        mGC.gc()
    }

    /**
     * Refresh the tile list in the synchronized way
     * so that the asynchronous running GC actually get the actual next tile
     * when calling method [.next]
     */
    private fun refresh() {
        synchronized(mTileAreas) {
            var index = 0
            for (area in mCache.additionalMapTileList.list) {
                val copy: MapTileArea
                if (index < mTileAreas.list.size) {
                    copy = mTileAreas.list.get(index)
                } else {
                    copy = MapTileArea()
                    mTileAreas.list.add(copy)
                }
                copy.set(area)
                index++
            }
            while (index < mTileAreas.list.size) {
                mTileAreas.list.removeAt(mTileAreas.list.size - 1)
            }
            mTileIndices = mTileAreas.iterator()
        }
    }

    /**
     * Get the next tile to search for
     *
     * @return -1 if not found
     */
    private fun next(): Long {
        while (true) {
            val index: Long
            synchronized(mTileAreas) {
                if (!mTileIndices!!.hasNext()) {
                    return -1
                }
                index = mTileIndices!!.next()!!
            }
            val drawable = mCache.getMapTile(index)
            if (drawable == null) {
                return index
            }
        }
    }

    /**
     * Search for a tile bitmap into the list of providers and put it in the memory cache
     */
    private fun search(pMapTileIndex: Long) {
        for (provider in mProviders) {
            try {
                if (provider is MapTileDownloader) {
                    val tileSource = provider.getTileSource()
                    if (tileSource is OnlineTileSourceBase) {
                        if (!tileSource.tileSourcePolicy.acceptsPreventive()) {
                            continue
                        }
                    }
                }
                val drawable = provider.getTileLoader().loadTileIfReachable(pMapTileIndex)
                if (drawable == null) {
                    continue
                }
                mCache.putTile(pMapTileIndex, drawable)
                return
            } catch (exception: CantContinueException) {
                // just dismiss that lazily: we don't need to be severe here
            }
        }
    }
}

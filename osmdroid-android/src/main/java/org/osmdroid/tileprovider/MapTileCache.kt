// Created by plusminus on 17:58:57 - 25.09.2008
package org.osmdroid.tileprovider

import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.util.MapTileArea
import org.osmdroid.util.MapTileAreaComputer
import org.osmdroid.util.MapTileAreaList
import org.osmdroid.util.MapTileContainer
import org.osmdroid.util.MapTileList

/**
 * In memory cache of tiles
 *
 * @author Nicolas Gramlich
 */
class MapTileCache @JvmOverloads constructor(aMaximumCacheSize: Int = instance!!.cacheMapTileCount.toInt()) {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    /**
     * @since 6.0.0
     * Was in LRUMapTileCache
     */
    interface TileRemovedListener {
        fun onTileRemoved(pMapTileIndex: Long)
    }

    /**
     * @since 6.0.0
     * Was in LRUMapTileCache
     */
    /**
     * @since 6.0.0
     * Was in LRUMapTileCache
     */
    var tileRemovedListener: TileRemovedListener? = null
    private val mCachedTiles = HashMap<Long?, Drawable?>()
    /**
     * @since 6.0.3
     */
    /**
     * Tiles currently displayed
     */
    val mapTileArea: MapTileArea = MapTileArea()
    /**
     * @since 6.0.3
     */
    /**
     * Tiles neighbouring the tiles currently displayed (borders, zoom +-1, ...)
     */
    val additionalMapTileList: MapTileAreaList = MapTileAreaList()

    /**
     * Tiles currently in the cache, without the concurrency side effects
     */
    private val mGC = MapTileList()

    /**
     * @since 6.0.2
     */
    val protectedTileComputers: MutableList<MapTileAreaComputer> = ArrayList<MapTileAreaComputer>()

    private var mCapacity = 0

    /**
     * @since 6.0.2
     */
    val preCache: MapTilePreCache

    /**
     * @since 6.0.2
     */
    /**
     * @since 6.0.2
     */
    val protectedTileContainers: MutableList<MapTileContainer> = ArrayList<MapTileContainer>()

    /**
     * @since 6.0.3
     */
    private var mAutoEnsureCapacity = false

    /**
     * @since 6.0.4
     */
    private var mStressedMemory = false

    /**
     * @param aMaximumCacheSize Maximum amount of MapTiles to be hold within.
     */
    // ===========================================================
    // Constructors
    // ===========================================================
    init {
        ensureCapacity(aMaximumCacheSize)
        this.preCache = MapTilePreCache(this)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    /**
     * @since 6.0.3
     */
    fun setAutoEnsureCapacity(pAutoEnsureCapacity: Boolean) {
        mAutoEnsureCapacity = pAutoEnsureCapacity
    }

    /**
     * @since 6.0.4
     * When true, all the tiles in the cache that eventually don't belong here are removed asap.
     * When false, we will still remove tiles that do not belong in the cache anymore,
     * but not necessarily all of them: only the amount we need in order to fit the cache size.
     * Should be set to true when you have small memory and big tiles in order to
     * avoid OutOfMemoryException.
     * Should be set to false for better performances.
     */
    fun setStressedMemory(pStressedMemory: Boolean) {
        mStressedMemory = pStressedMemory
    }

    fun ensureCapacity(pCapacity: Int): Boolean {
        if (mCapacity < pCapacity) {
            Log.i(IMapView.LOGTAG, "Tile cache increased from " + mCapacity + " to " + pCapacity)
            mCapacity = pCapacity
            return true
        }
        return false
    }

    fun getMapTile(pMapTileIndex: Long): Drawable? {
        synchronized(mCachedTiles) {
            return this.mCachedTiles.get(pMapTileIndex)
        }
    }

    fun putTile(pMapTileIndex: Long, aDrawable: Drawable?) {
        if (aDrawable != null) {
            synchronized(mCachedTiles) {
                this.mCachedTiles.put(pMapTileIndex, aDrawable)
            }
        }
    }

    /**
     * Removes from the memory cache all the tiles that should no longer be there
     *
     * @since 6.0.0
     */
    fun garbageCollection() {
        // number of tiles to remove from cache
        var toBeRemoved = Int.Companion.MAX_VALUE // MAX_VALUE for stressed memory case
        val size = mCachedTiles.size
        if (!mStressedMemory) {
            toBeRemoved = size - mCapacity
            if (toBeRemoved <= 0) {
                return
            }
        }

        refreshAdditionalLists()

        if (mAutoEnsureCapacity) {
            val target = mapTileArea.size() + additionalMapTileList.size()
            if (ensureCapacity(target)) {
                if (!mStressedMemory) {
                    toBeRemoved = size - mCapacity
                    if (toBeRemoved <= 0) {
                        return
                    }
                }
            }
        }
        populateSyncCachedTiles(mGC)
        for (i in 0 until mGC.size) {
            val index = mGC.get(i)
            if (shouldKeepTile(index)) {
                continue
            }
            remove(index)
            if (--toBeRemoved == 0) {
                break
            }
        }
    }

    /**
     * @since 6.0.3
     */
    private fun refreshAdditionalLists() {
        var index = 0
        for (computer in this.protectedTileComputers) {
            val area: MapTileArea?
            if (index < additionalMapTileList.list.size) {
                area = additionalMapTileList.list.get(index)
            } else {
                area = MapTileArea()
                additionalMapTileList.list.add(area)
            }
            computer.computeFromSource(this.mapTileArea, area)
            index++
        }
        while (index < additionalMapTileList.list.size) {
            additionalMapTileList.list.removeAt(additionalMapTileList.list.size - 1)
        }
    }

    /**
     * @since 6.0.2
     */
    private fun shouldKeepTile(pMapTileIndex: Long): Boolean {
        if (mapTileArea.contains(pMapTileIndex)) {
            return true
        }
        if (additionalMapTileList.contains(pMapTileIndex)) {
            return true
        }
        for (container in this.protectedTileContainers) {
            if (container.contains(pMapTileIndex)) {
                return true
            }
        }
        return false
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    // ===========================================================
    // Methods
    // ===========================================================
    fun containsTile(pMapTileIndex: Long): Boolean {
        synchronized(mCachedTiles) {
            return this.mCachedTiles.containsKey(pMapTileIndex)
        }
    }

    /**
     * @since 6.0.0
     * Was in LRUMapTileCache
     */
    fun clear() {
        // remove them all individually so that they get recycled
        val list = MapTileList()
        populateSyncCachedTiles(list)
        for (i in 0 until list.size) {
            val index = list.get(i)
            remove(index)
        }

        // and then clear
        mCachedTiles.clear()
    }

    /**
     * @since 6.0.0
     * Was in LRUMapTileCache
     */
    fun remove(pMapTileIndex: Long) {
        val drawable: Drawable?
        synchronized(mCachedTiles) {
            drawable = mCachedTiles.remove(pMapTileIndex)
        }
        if (this.tileRemovedListener != null) this.tileRemovedListener!!.onTileRemoved(pMapTileIndex)
        BitmapPool.instance.asyncRecycle(drawable)
    }

    /**
     * Just a helper method in order to parse all indices without concurrency side effects
     *
     * @since 6.0.0
     */
    private fun populateSyncCachedTiles(pList: MapTileList) {
        synchronized(mCachedTiles) {
            pList.ensureCapacity(mCachedTiles.size)
            pList.clear()
            for (index in mCachedTiles.keys) {
                pList.put(index!!)
            }
        }
    }

    val size: Int
        /**
         * @since 6.0.0
         */
        get() = mCachedTiles.size

    /**
     * Maintenance operations
     *
     * @since 6.0.2
     */
    fun maintenance() {
        garbageCollection()
        preCache.fill()
    }
}

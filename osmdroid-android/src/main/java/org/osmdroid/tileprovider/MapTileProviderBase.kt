package org.osmdroid.tileprovider

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.RectL
import org.osmdroid.util.TileLooper
import org.osmdroid.util.TileSystem
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.TilesOverlay
import kotlin.math.abs

/**
 * This is an abstract class. The tile provider is responsible for:
 *
 *  * determining if a map tile is available,
 *  * notifying the client, via a callback handler
 *
 * see [MapTileIndex] for an overview of how tiles are served by this provider.
 *
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author plusminus on 21:46:22 - 25.09.2008
 * @author and many other contributors
 */
abstract class MapTileProviderBase @JvmOverloads constructor(
    pTileSource: ITileSource?,
    pDownloadFinishedListener: Handler? = null
) : IMapTileProviderCallback {
    /**
     * @since 6.0.0
     */
    val tileCache: MapTileCache

    /**
     * @since 6.1.0
     */
    val tileRequestCompleteHandlers: MutableCollection<Handler?> = LinkedHashSet<Handler?>()
    protected var mUseDataConnection: Boolean = true
    protected var mTileNotFoundImage: Drawable? = null

    private var mTileSource: ITileSource?

    /**
     * Attempts to get a Drawable that represents a [MapTileIndex]. If the tile is not immediately
     * available this will return null and attempt to get the tile from known tile sources for
     * subsequent future requests. Note that this may return a [ReusableBitmapDrawable] in
     * which case you should follow proper handling procedures for using that Drawable or it may
     * reused while you are working with it.
     *
     * @see ReusableBitmapDrawable
     */
    abstract fun getMapTile(pMapTileIndex: Long): Drawable?

    /**
     * classes that extend MapTileProviderBase must call this method to prevent memory leaks.
     * Updated 5.2+
     */
    open fun detach() {
        clearTileCache()
        if (mTileNotFoundImage != null) {
            // Only recycle if we are running on a project less than 2.3.3 Gingerbread.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                if (mTileNotFoundImage is BitmapDrawable) {
                    val bitmap = (mTileNotFoundImage as BitmapDrawable).getBitmap()
                    if (bitmap != null) {
                        bitmap.recycle()
                    }
                }
            }
            if (mTileNotFoundImage is ReusableBitmapDrawable) BitmapPool.instance
                .returnDrawableToPool(mTileNotFoundImage as ReusableBitmapDrawable)
        }
        mTileNotFoundImage = null
        clearTileCache()
    }

    /**
     * Gets the minimum zoom level this tile provider can provide
     *
     * @return the minimum zoom level
     */
    abstract fun getMinimumZoomLevel(): Int

    /**
     * Gets the maximum zoom level this tile provider can provide
     *
     * @return the maximum zoom level
     */
    abstract fun getMaximumZoomLevel(): Int

    open fun getTileSource(): ITileSource? = mTileSource

    open fun setTileSource(pTileSource: ITileSource?) {
        mTileSource = pTileSource
        clearTileCache()
    }

    /**
     * Creates a [MapTileCache] to be used to cache tiles in memory.
     */
    fun createTileCache(): MapTileCache {
        return MapTileCache()
    }

    init {
        this.tileCache = this.createTileCache()
        tileRequestCompleteHandlers.add(pDownloadFinishedListener)
        mTileSource = pTileSource
    }

    /**
     * Sets the "sorry we can't load a tile for this location" image. If it's null, the default view
     * is shown, which is the standard grey grid controlled by the tiles overlay
     * [TilesOverlay.setLoadingLineColor] and
     * [TilesOverlay.setLoadingBackgroundColor]
     *
     * @param drawable
     * @since 5.2+
     */
    fun setTileLoadFailureImage(drawable: Drawable?) {
        this.mTileNotFoundImage = drawable
    }

    /**
     * Called by implementation class methods indicating that they have completed the request as
     * best it can. The tile is added to the cache, and a MAPTILE_SUCCESS_ID message is sent.
     *
     * @param pState    the map tile request state object
     * @param pDrawable the Drawable of the map tile
     */
    override fun mapTileRequestCompleted(pState: MapTileRequestState?, pDrawable: Drawable?) {
        if (pState == null) return
        // put the tile in the cache
        putTileIntoCache(pState.mapTile, pDrawable, ExpirableBitmapDrawable.Companion.UP_TO_DATE)

        // tell our caller we've finished and it should update its view
        sendMessage(MAPTILE_SUCCESS_ID)

        if (instance!!.isDebugTileProviders) {
            Log.d(IMapView.LOGTAG, "MapTileProviderBase.mapTileRequestCompleted(): " + MapTileIndex.toString(pState.mapTile))
        }
    }

    /**
     * Called by implementation class methods indicating that they have failed to retrieve the
     * requested map tile. a MAPTILE_FAIL_ID message is sent.
     *
     * @param pState the map tile request state object
     */
    override fun mapTileRequestFailed(pState: MapTileRequestState?) {
        if (pState == null) return
        if (mTileNotFoundImage != null) {
            putTileIntoCache(pState.mapTile, mTileNotFoundImage, ExpirableBitmapDrawable.Companion.NOT_FOUND)
            sendMessage(MAPTILE_SUCCESS_ID)
        } else {
            sendMessage(MAPTILE_FAIL_ID)
        }
        if (instance!!.isDebugTileProviders) {
            Log.d(IMapView.LOGTAG, "MapTileProviderBase.mapTileRequestFailed(): " + MapTileIndex.toString(pState.mapTile))
        }
    }

    /**
     * Called by implementation class methods indicating that they have failed to retrieve the
     * requested map tile, because the max queue size has been reached
     *
     * @param pState the map tile request state object
     */
    override fun mapTileRequestFailedExceedsMaxQueueSize(pState: MapTileRequestState?) {
        mapTileRequestFailed(pState)
    }

    /**
     * Called by implementation class methods indicating that they have produced an expired result
     * that can be used but better results may be delivered later. The tile is added to the cache,
     * and a MAPTILE_SUCCESS_ID message is sent.
     *
     * @param pState    the map tile request state object
     * @param pDrawable the Drawable of the map tile
     */
    override fun mapTileRequestExpiredTile(pState: MapTileRequestState?, pDrawable: Drawable?) {
        if (pState == null || pDrawable == null) return
        putTileIntoCache(pState.mapTile, pDrawable, ExpirableBitmapDrawable.Companion.getState(pDrawable))

        // tell our caller we've finished and it should update its view
        sendMessage(MAPTILE_SUCCESS_ID)

        if (instance!!.isDebugTileProviders) {
            Log.d(IMapView.LOGTAG, "MapTileProviderBase.mapTileRequestExpiredTile(): " + MapTileIndex.toString(pState.mapTile))
        }
    }

    /**
     * @since 5.6.5
     */
    protected fun putTileIntoCache(pMapTileIndex: Long, pDrawable: Drawable?, pState: Int) {
        if (pDrawable == null) {
            return
        }
        val before = tileCache.getMapTile(pMapTileIndex)
        if (before != null) {
            val stateBefore: Int = ExpirableBitmapDrawable.Companion.getState(before)
            if (stateBefore > pState) {
                return
            }
        }
        ExpirableBitmapDrawable.Companion.setState(pDrawable, pState)
        tileCache.putTile(pMapTileIndex, pDrawable)
    }

    @Deprecated("Use {@link #putTileIntoCache(long, Drawable, int)}} instead")
    protected fun putExpiredTileIntoCache(pState: MapTileRequestState, pDrawable: Drawable?) {
        putTileIntoCache(pState.mapTile, pDrawable, ExpirableBitmapDrawable.Companion.EXPIRED)
    }

    @Deprecated("Use {@link #getTileRequestCompleteHandlers()} instead")
    fun setTileRequestCompleteHandler(handler: Handler?) {
        tileRequestCompleteHandlers.clear()
        tileRequestCompleteHandlers.add(handler)
    }

    fun ensureCapacity(pCapacity: Int) {
        tileCache.ensureCapacity(pCapacity)
    }

    /**
     * purges the cache of all tiles (default is the in memory cache)
     */
    fun clearTileCache() {
        tileCache.clear()
    }

    /**
     * Whether to use the network connection if it's available.
     */
    override fun useDataConnection(): Boolean {
        return mUseDataConnection
    }

    /**
     * Set whether to use the network connection if it's available.
     *
     * @param pMode if true use the network connection if it's available. if false don't use the
     * network connection even if it's available.
     */
    fun setUseDataConnection(pMode: Boolean) {
        mUseDataConnection = pMode
    }

    /**
     * Recreate the cache using scaled versions of the tiles currently in it
     *
     * @param pNewZoomLevel the zoom level that we need now
     * @param pOldZoomLevel the previous zoom level that we should get the tiles to rescale
     * @param pViewPort     the view port we need tiles for
     */
    fun rescaleCache(
        pProjection: Projection, pNewZoomLevel: Double,
        pOldZoomLevel: Double, pViewPort: Rect
    ) {
        if (TileSystem.Companion.getInputTileZoomLevel(pNewZoomLevel) == TileSystem.Companion.getInputTileZoomLevel(pOldZoomLevel)) {
            return
        }

        val startMs = System.currentTimeMillis()
        if (instance!!.isDebugTileProviders) Log.i(IMapView.LOGTAG, "rescale tile cache from " + pOldZoomLevel + " to " + pNewZoomLevel)

        val topLeftMercator = pProjection.toMercatorPixels(pViewPort.left, pViewPort.top, null)
        val bottomRightMercator = pProjection.toMercatorPixels(
            pViewPort.right, pViewPort.bottom,
            null
        )
        val viewPortMercator = RectL(
            topLeftMercator.x, topLeftMercator.y, bottomRightMercator.x, bottomRightMercator.y
        )

        val tileLooper = if (pNewZoomLevel > pOldZoomLevel)
            ZoomInTileLooper()
        else
            ZoomOutTileLooper()
        tileLooper.loop(pNewZoomLevel, viewPortMercator, pOldZoomLevel, getTileSource()!!.tileSizePixels)

        val endMs = System.currentTimeMillis()
        if (instance!!.isDebugTileProviders) Log.i(IMapView.LOGTAG, "Finished rescale in " + (endMs - startMs) + "ms")
    }

    private abstract inner class ScaleTileLooper : TileLooper() {
        /**
         * new (scaled) tiles to add to cache
         * NB first generate all and then put all in cache,
         * otherwise the ones we need will be pushed out
         */
        protected val mNewTiles: HashMap<Long?, Bitmap?> = HashMap<Long?, Bitmap?>()

        protected var mOldTileZoomLevel: Int = 0
        protected var mTileSize: Int = 0
        protected var mDiff: Int = 0
        protected var mTileSize_2: Int = 0
        protected var mSrcRect: Rect? = null
        protected var mDestRect: Rect? = null
        protected var mDebugPaint: Paint? = null
        private var isWorth = false

        fun loop(pZoomLevel: Double, pViewPortMercator: RectL, pOldZoomLevel: Double, pTileSize: Int) {
            mSrcRect = Rect()
            mDestRect = Rect()
            mDebugPaint = Paint()
            mOldTileZoomLevel = TileSystem.Companion.getInputTileZoomLevel(pOldZoomLevel)
            mTileSize = pTileSize
            loop(pZoomLevel, pViewPortMercator)
        }

        override fun initialiseLoop() {
            super.initialiseLoop()
            mDiff = abs(mTileZoomLevel - mOldTileZoomLevel)
            mTileSize_2 = mTileSize shr mDiff
            isWorth = mDiff != 0
        }

        override fun handleTile(pMapTileIndex: Long, pX: Int, pY: Int) {
            if (!isWorth) {
                return
            }

            // Get tile from cache.
            // If it's found then no need to created scaled version.
            // If not found (null) them we've initiated a new request for it,
            // and now we'll create a scaled version until the request completes.
            val requestedTile = getMapTile(pMapTileIndex)
            if (requestedTile == null) {
                try {
                    computeTile(pMapTileIndex, pX, pY)
                } catch (e: OutOfMemoryError) {
                    Log.e(IMapView.LOGTAG, "OutOfMemoryError rescaling cache")
                }
            }
        }

        override fun finaliseLoop() {
            // now add the new ones, pushing out the old ones
            while (!mNewTiles.isEmpty()) {
                val index = mNewTiles.keys.iterator().next()!!
                val bitmap = mNewTiles.remove(index)
                putScaledTileIntoCache(index, bitmap!!)
            }
        }

        protected abstract fun computeTile(pMapTileIndex: Long, pX: Int, pY: Int)

        /**
         * @since 5.6.5
         */
        protected fun putScaledTileIntoCache(pMapTileIndex: Long, pBitmap: Bitmap) {
            val drawable = ReusableBitmapDrawable(pBitmap)
            putTileIntoCache(pMapTileIndex, drawable, ExpirableBitmapDrawable.Companion.SCALED)
            if (instance!!.isDebugMode) {
                Log.d(IMapView.LOGTAG, "Created scaled tile: " + MapTileIndex.toString(pMapTileIndex))
                mDebugPaint!!.setTextSize(40f)
                val canvas = Canvas(pBitmap)
                canvas.drawText("scaled", 50f, 50f, mDebugPaint!!)
            }
        }
    }

    private inner class ZoomInTileLooper : ScaleTileLooper() {
        public override fun computeTile(pMapTileIndex: Long, pX: Int, pY: Int) {
            // get the correct fraction of the tile from cache and scale up

            val oldTile = MapTileIndex.getTileIndex(
                mOldTileZoomLevel,
                MapTileIndex.getX(pMapTileIndex) shr mDiff, MapTileIndex.getY(pMapTileIndex) shr mDiff
            )
            val oldDrawable = tileCache.getMapTile(oldTile)

            if (oldDrawable is BitmapDrawable) {
                val bitmap: Bitmap? = MapTileApproximater.Companion.approximateTileFromLowerZoom(
                    oldDrawable, pMapTileIndex, mDiff
                )
                if (bitmap != null) {
                    mNewTiles.put(pMapTileIndex, bitmap)
                }
            }
        }
    }

    private inner class ZoomOutTileLooper : ScaleTileLooper() {
        override fun computeTile(pMapTileIndex: Long, pX: Int, pY: Int) {
            if (mDiff >= MAX_ZOOM_OUT_DIFF) {
                return
            }

            // get many tiles from cache and make one tile from them
            val xx = MapTileIndex.getX(pMapTileIndex) shl mDiff
            val yy = MapTileIndex.getY(pMapTileIndex) shl mDiff
            val numTiles = 1 shl mDiff
            var bitmap: Bitmap? = null
            var canvas: Canvas? = null
            for (x in 0 until numTiles) {
                for (y in 0 until numTiles) {
                    val oldTile = MapTileIndex.getTileIndex(mOldTileZoomLevel, xx + x, yy + y)
                    val oldDrawable = tileCache.getMapTile(oldTile)
                    if (oldDrawable is BitmapDrawable) {
                        val oldBitmap = oldDrawable.getBitmap()
                        if (oldBitmap != null) {
                            if (bitmap == null) {
                                bitmap = MapTileApproximater.Companion.getTileBitmap(mTileSize)
                                canvas = Canvas(bitmap!!)
                                canvas.drawColor(sApproximationBackgroundColor)
                            }
                            mDestRect!!.set(
                                x * mTileSize_2, y * mTileSize_2,
                                (x + 1) * mTileSize_2, (y + 1) * mTileSize_2
                            )
                            canvas!!.drawBitmap(oldBitmap, null, mDestRect!!, null)
                        }
                    }
                }
            }

            if (bitmap != null) {
                mNewTiles.put(pMapTileIndex, bitmap)
            }
        }

    }


    abstract fun getTileWriter(): IFilesystemCache?

    /**
     * @return the number of tile requests currently in the queue
     * @since 5.6
     */
    abstract fun getQueueSize(): Long

    /**
     * Expire a tile that is in the memory cache
     * Typical use is for mapsforge, where the contents of the tile can evolve,
     * depending on the neighboring tiles that have been displayed so far.
     *
     * @since 6.0.3
     */
    fun expireInMemoryCache(pMapTileIndex: Long) {
        val drawable = tileCache.getMapTile(pMapTileIndex)
        if (drawable != null) {
            ExpirableBitmapDrawable.Companion.setState(drawable, ExpirableBitmapDrawable.Companion.EXPIRED)
        }
    }

    /**
     * Concurrency exception management (cf. https://github.com/osmdroid/osmdroid/issues/1446)
     * Given the likelihood of consecutive ConcurrentModificationException's,
     * we just try again and 3 attempts are supposedly enough.
     *
     * @since 6.2.0
     */
    private fun sendMessage(pMessageId: Int) {
        for (attempt in 0..2) {
            if (sendMessageFailFast(pMessageId)) {
                return
            }
        }
    }

    /**
     * Concurrency exception management (cf. https://github.com/osmdroid/osmdroid/issues/1446)
     * Of course a for-each loop would make sense, but it's prone to concurrency issues.
     *
     * @return false if a ConcurrentModificationException was thrown
     * @since 6.2.0
     */
    private fun sendMessageFailFast(pMessageId: Int): Boolean {
        val iterator = tileRequestCompleteHandlers.iterator()
        while (iterator.hasNext()) {
            val handler: Handler?
            try {
                handler = iterator.next()
            } catch (cme: ConcurrentModificationException) {
                return false
            }
            if (handler != null) {
                handler.sendEmptyMessage(pMessageId)
            }
        }
        return true
    }

    companion object {
        private const val MAX_ZOOM_OUT_DIFF = 4
        const val MAPTILE_SUCCESS_ID: Int = 0
        val MAPTILE_FAIL_ID: Int = MAPTILE_SUCCESS_ID + 1

        private var sApproximationBackgroundColor = Color.LTGRAY

        /**
         * Sets the default color for approximated tiles.
         *
         * @param pColor the default color that will be shown for approximated tiles
         */
        fun setApproximationBackgroundColor(pColor: Int) {
            sApproximationBackgroundColor = pColor
        }
    }
}

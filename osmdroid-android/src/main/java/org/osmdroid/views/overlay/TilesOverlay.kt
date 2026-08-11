package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.library.R
import org.osmdroid.tileprovider.BitmapPool
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.ReusableBitmapDrawable
import org.osmdroid.tileprovider.TileStates
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.RectL
import org.osmdroid.util.TileLooper
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.drawing.MapSnapshot.MapSnapshotable
import java.io.File

/**
 * A [TilesOverlay] is responsible to display a [MapTileIndex].
 *
 *
 * These objects are the principle consumer of map tiles.
 *
 *
 * see [MapTileIndex] for an overview of how tiles are acquired by this overlay.
 */
open class TilesOverlay @JvmOverloads constructor(
    aTileProvider: MapTileProviderBase,
    private var ctx: Context?,
    horizontalWrapEnabled: Boolean = true,
    verticalWrapEnabled: Boolean = true
) : Overlay(), IOverlayMenuProvider {
    /**
     * Current tile source
     */
    protected val mTileProvider: MapTileProviderBase

    protected var userSelectedLoadingDrawable: Drawable? = null

    /* to avoid allocations during draw */
    protected val mDebugPaint: Paint = Paint()
    private val mTileRect = Rect()
    protected val mViewPort: RectL = RectL()

    protected var mProjection: Projection? = null
    override var isOptionsMenuEnabled: Boolean = true

    /**
     * A drawable loading tile
     */
    private var mLoadingTile: BitmapDrawable? = null
    private var mLoadingBackgroundColor = Color.rgb(216, 208, 208)
    private var mLoadingLineColor = Color.rgb(200, 192, 192)

    var isHorizontalWrapEnabled: Boolean = true
        set(horizontalWrapEnabled) {
            field = horizontalWrapEnabled
            this.mTileLooper.isHorizontalWrapEnabled = horizontalWrapEnabled
        }
    var isVerticalWrapEnabled: Boolean = true
        set(verticalWrapEnabled) {
            field = verticalWrapEnabled
            this.mTileLooper.isVerticalWrapEnabled = verticalWrapEnabled
        }

    //Issue 133 night mode
    private var currentColorFilter: ColorFilter? = null

    /**
     * @since 6.1.0
     */
    private val mProtectedTiles = Rect() // optimization

    /**
     * @since 6.1.0
     */
    /**
     * @since 6.1.0
     */
    val tileStates: TileStates = TileStates()

    /**
     * See issue https://github.com/osmdroid/osmdroid/issues/330
     * customizable override for the grey grid
     *
     * @param drawable
     * @since 5.2+
     */
    fun setLoadingDrawable(drawable: Drawable?) {
        userSelectedLoadingDrawable = drawable
    }

    override fun onDetach(pMapView: MapView?) {
        this.mTileProvider.detach()
        ctx = null
        BitmapPool.instance.asyncRecycle(mLoadingTile)
        mLoadingTile = null
        BitmapPool.instance.asyncRecycle(userSelectedLoadingDrawable)
        userSelectedLoadingDrawable = null
    }

    val minimumZoomLevel: Int
        get() = mTileProvider.getMinimumZoomLevel()

    val maximumZoomLevel: Int
        get() = mTileProvider.getMaximumZoomLevel()

    /**
     * Whether to use the network connection if it's available.
     */
    fun useDataConnection(): Boolean {
        return mTileProvider.useDataConnection()
    }

    /**
     * Set whether to use the network connection if it's available.
     *
     * @param aMode if true use the network connection if it's available. if false don't use the
     * network connection even if it's available.
     */
    fun setUseDataConnection(aMode: Boolean) {
        mTileProvider.setUseDataConnection(aMode)
    }

    /**
     * Populates the tile provider's memory cache with the list of displayed tiles
     *
     * @since 6.0.0
     */
    fun protectDisplayedTilesForCache(pCanvas: Canvas, pProjection: Projection) {
        if (!setViewPort(pCanvas, pProjection)) {
            return
        }
        TileSystem.Companion.getTileFromMercator(mViewPort, TileSystem.Companion.getTileSize(mProjection!!.zoomLevel), mProtectedTiles)
        val tileZoomLevel: Int = TileSystem.Companion.getInputTileZoomLevel(mProjection!!.zoomLevel)
        mTileProvider.tileCache.mapTileArea.set(tileZoomLevel, mProtectedTiles)
        mTileProvider.tileCache.maintenance()
    }

    /**
     * Get the area we are drawing to
     *
     * @return true if the tiles are to be drawn
     * @since 6.0.0
     */
    protected open fun setViewPort(pCanvas: Canvas, pProjection: Projection): Boolean {
        this.projection = pProjection
        this.projection.getMercatorViewPort(mViewPort)
        return true
    }

    override fun draw(c: Canvas, pProjection: Projection) {
        if (instance!!.isDebugTileProviders) {
            Log.d(IMapView.LOGTAG, "onDraw")
        }

        if (!setViewPort(c, pProjection)) {
            return
        }

        // Draw the tiles!
        drawTiles(c, this.projection, this.projection.zoomLevel, mViewPort)
    }

    /**
     * This is meant to be a "pure" tile drawing function that doesn't take into account
     * osmdroid-specific characteristics (like osmdroid's canvas's having 0,0 as the center rather
     * than the upper-left corner). Once the tile is ready to be drawn, it is passed to
     * onTileReadyToDraw where custom manipulations can be made before drawing the tile.
     */
    fun drawTiles(c: Canvas?, projection: Projection, zoomLevel: Double, viewPort: RectL) {
        mProjection = projection
        mTileLooper.loop(zoomLevel, viewPort, c)
    }

    /**
     * @since 6.0
     */
    protected inner class OverlayTileLooper : TileLooper {
        private var mCanvas: Canvas? = null

        constructor() : super()

        constructor(horizontalWrapEnabled: Boolean, verticalWrapEnabled: Boolean) : super(horizontalWrapEnabled, verticalWrapEnabled)

        fun loop(pZoomLevel: Double, pViewPort: RectL, pCanvas: Canvas?) {
            mCanvas = pCanvas
            loop(pZoomLevel, pViewPort)
        }

        override fun initialiseLoop() {
            // make sure the cache is big enough for all the tiles
            val width = mTiles.right - mTiles.left + 1
            val height = mTiles.bottom - mTiles.top + 1
            val numNeeded = height * width
            mTileProvider.ensureCapacity(numNeeded + instance!!.cacheMapTileOvershoot)
            tileStates.initialiseLoop()
            super.initialiseLoop()
        }

        override fun handleTile(pMapTileIndex: Long, pX: Int, pY: Int) {
            var currentMapTile = mTileProvider.getMapTile(pMapTileIndex)
            tileStates.handleTile(currentMapTile)
            if (mCanvas == null) { // in case we just want to have the tiles downloaded, not displayed
                return
            }
            var isReusable = currentMapTile is ReusableBitmapDrawable
            val reusableBitmapDrawable =
                if (isReusable) currentMapTile as ReusableBitmapDrawable else null
            if (currentMapTile == null) {
                currentMapTile = this@TilesOverlay.loadingTile
            }

            if (currentMapTile != null) {
                mProjection!!.getPixelFromTile(pX, pY, mTileRect)
                if (isReusable) {
                    reusableBitmapDrawable!!.beginUsingDrawable()
                }
                try {
                    if (isReusable && !reusableBitmapDrawable!!.isBitmapValid) {
                        currentMapTile = this@TilesOverlay.loadingTile
                        isReusable = false
                    }
                    onTileReadyToDraw(mCanvas!!, currentMapTile!!, mTileRect)
                } finally {
                    if (isReusable) reusableBitmapDrawable!!.finishUsingDrawable()
                }
            }

            if (instance!!.isDebugTileProviders) {
                mProjection!!.getPixelFromTile(pX, pY, mTileRect)
                mCanvas!!.drawText(
                    MapTileIndex.toString(pMapTileIndex), (mTileRect.left + 1).toFloat(),
                    mTileRect.top + mDebugPaint.getTextSize(), mDebugPaint
                )
                mCanvas!!.drawLine(
                    mTileRect.left.toFloat(), mTileRect.top.toFloat(), mTileRect.right.toFloat(), mTileRect.top.toFloat(),
                    mDebugPaint
                )
                mCanvas!!.drawLine(
                    mTileRect.left.toFloat(), mTileRect.top.toFloat(), mTileRect.left.toFloat(), mTileRect.bottom.toFloat(),
                    mDebugPaint
                )
            }
        }

        override fun finaliseLoop() {
            tileStates.finaliseLoop()
        }
    }

    private val mTileLooper = OverlayTileLooper()
    private val mIntersectionRect = Rect()

    protected var canvasRect: Rect? = null

    init {
        requireNotNull(aTileProvider) { "You must pass a valid tile provider to the tiles overlay." }
        this.mTileProvider = aTileProvider
        this.isHorizontalWrapEnabled = horizontalWrapEnabled
        this.isVerticalWrapEnabled = verticalWrapEnabled
    }

    protected var projection: Projection
        get() = mProjection!!
        protected set(pProjection) {
            mProjection = pProjection
        }


    protected fun onTileReadyToDraw(c: Canvas, currentMapTile: Drawable, tileRect: Rect) {
        currentMapTile.setColorFilter(currentColorFilter)
        currentMapTile.setBounds(tileRect.left, tileRect.top, tileRect.right, tileRect.bottom)
        val canvasRect = this.canvasRect
        if (canvasRect == null) {
            currentMapTile.draw(c)
            return
        }
        // Check to see if the drawing area intersects with the minimap area
        if (!mIntersectionRect.setIntersect(c.getClipBounds(), canvasRect)) {
            return
        }
        // Save the current clipping bounds
        c.save()

        // Clip that area
        c.clipRect(mIntersectionRect)

        // Draw the tile, which will be appropriately clipped
        currentMapTile.draw(c)

        c.restore()
    }

    override fun onCreateOptionsMenu(
        pMenu: Menu?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        pMenu ?: return false
        pMapView ?: return false
        val mapMenu = pMenu.addSubMenu(
            0, Menu.NONE, Menu.NONE,
            R.string.map_mode
        ).setIcon(R.drawable.ic_menu_mapmode)

        for (a in TileSourceFactory.tileSources.indices) {
            val tileSource = TileSourceFactory.tileSources[a]
            mapMenu.add(
                MENU_MAP_MODE + pMenuIdOffset, (MENU_TILE_SOURCE_STARTING_ID + a
                        + pMenuIdOffset), Menu.NONE, tileSource.name()
            )
        }
        mapMenu.setGroupCheckable(MENU_MAP_MODE + pMenuIdOffset, true, true)

        if (ctx != null) {
            val title = ctx!!.getString(
                if (pMapView.useDataConnection())
                    R.string.set_mode_offline
                else
                    R.string.set_mode_online
            )
            val icon = ctx!!.getResources().getDrawable(R.drawable.ic_menu_offline)
            pMenu.add(0, MENU_OFFLINE + pMenuIdOffset, Menu.NONE, title).setIcon(icon)
            pMenu.add(0, MENU_SNAPSHOT + pMenuIdOffset, Menu.NONE, R.string.snapshot)
            pMenu.add(0, MENU_STATES + pMenuIdOffset, Menu.NONE, R.string.states)
        }
        return true
    }

    override fun onPrepareOptionsMenu(
        pMenu: Menu?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        pMenu ?: return false
        pMapView ?: return false
        val index = TileSourceFactory.tileSources.indexOf(
            pMapView.getTileProvider()!!.getTileSource()
        )
        if (index >= 0) {
            pMenu.findItem(MENU_TILE_SOURCE_STARTING_ID + index + pMenuIdOffset).setChecked(true)
        }

        pMenu.findItem(MENU_OFFLINE + pMenuIdOffset).setTitle(
            if (pMapView.useDataConnection())
                R.string.set_mode_offline
            else
                R.string.set_mode_online
        )

        return true
    }

    override fun onOptionsItemSelected(
        pItem: MenuItem?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        pItem ?: return false
        pMapView ?: return false
        val menuId = pItem.getItemId() - pMenuIdOffset
        if ((menuId >= MENU_TILE_SOURCE_STARTING_ID)
            && (menuId < MENU_TILE_SOURCE_STARTING_ID
                    + TileSourceFactory.tileSources.size)
        ) {
            pMapView.setTileSource(
                TileSourceFactory.tileSources[
                    menuId - MENU_TILE_SOURCE_STARTING_ID
                ]
            )
            return true
        } else if (menuId == MENU_OFFLINE) {
            val useDataConnection = !pMapView.useDataConnection()
            pMapView.setUseDataConnection(useDataConnection)
            return true
        } else if (menuId == MENU_STATES) {
            Toast.makeText(pMapView.getContext(), tileStates.toString(), Toast.LENGTH_SHORT).show()
            return true
        } else if (menuId == MENU_SNAPSHOT) {
            val mapSnapshot = MapSnapshot(object : MapSnapshotable {
                override fun callback(pMapSketch: MapSnapshot?) {
                    pMapSketch ?: return
                    if (pMapSketch.status != MapSnapshot.Status.CANVAS_OK) {
                        return
                    }
                    val file = File(instance!!.osmdroidBasePath, "snapshot.png")
                    pMapSketch.save(file)
                    pMapSketch.onDetach()
                }
            }, MapSnapshot.Companion.INCLUDE_FLAG_UPTODATE, pMapView)
            val t = Thread(mapSnapshot)
            t.setName("TilesOverlaySnapShotThread")
            t.start()
            return true
        } else {
            return false
        }
    }

    var loadingBackgroundColor: Int
        get() = mLoadingBackgroundColor
        /**
         * Set the color to use to draw the background while we're waiting for the tile to load.
         *
         * @param pLoadingBackgroundColor the color to use. If the value is [Color.TRANSPARENT] then there will be no
         * loading tile.
         */
        set(pLoadingBackgroundColor) {
            if (mLoadingBackgroundColor != pLoadingBackgroundColor) {
                mLoadingBackgroundColor = pLoadingBackgroundColor
                clearLoadingTile()
            }
        }

    var loadingLineColor: Int
        get() = mLoadingLineColor
        set(pLoadingLineColor) {
            if (mLoadingLineColor != pLoadingLineColor) {
                mLoadingLineColor = pLoadingLineColor
                clearLoadingTile()
            }
        }

    private val loadingTile: Drawable?
        get() {
            if (userSelectedLoadingDrawable != null) return userSelectedLoadingDrawable
            if (mLoadingTile == null && mLoadingBackgroundColor != Color.TRANSPARENT) {
                try {
                    val tileSource = mTileProvider.getTileSource()
                    val tileSize = tileSource?.tileSizePixels ?: 256
                    val bitmap = Bitmap.createBitmap(
                        tileSize, tileSize,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    val paint = Paint()
                    canvas.drawColor(mLoadingBackgroundColor)
                    paint.setColor(mLoadingLineColor)
                    paint.setStrokeWidth(0f)
                    val lineSize = tileSize / 16
                    var a = 0
                    while (a < tileSize) {
                        canvas.drawLine(0f, a.toFloat(), tileSize.toFloat(), a.toFloat(), paint)
                        canvas.drawLine(a.toFloat(), 0f, a.toFloat(), tileSize.toFloat(), paint)
                        a += lineSize
                    }
                    mLoadingTile = BitmapDrawable(bitmap)
                } catch (e: OutOfMemoryError) {
                    Log.e(IMapView.LOGTAG, "OutOfMemoryError getting loading tile")
                    System.gc()
                } catch (e: NullPointerException) {
                    Log.e(IMapView.LOGTAG, "NullPointerException getting loading tile")
                    System.gc()
                }
            }
            return mLoadingTile
        }

    private fun clearLoadingTile() {
        val bitmapDrawable = mLoadingTile
        mLoadingTile = null
        BitmapPool.instance.asyncRecycle(bitmapDrawable)
    }


    /**
     * sets the current color filter, which is applied to tiles before being drawn to the screen.
     * Use this to enable night mode or any other tile rendering adjustment as necessary. use null to clear.
     * INVERT_COLORS provides color inversion for convenience and to support the previous night mode
     *
     * @param filter
     * @since 5.1
     */
    fun setColorFilter(filter: ColorFilter?) {
        this.currentColorFilter = filter
    }

    companion object {
        val MENU_MAP_MODE: Int = getSafeMenuId()
        val MENU_TILE_SOURCE_STARTING_ID: Int = getSafeMenuIdSequence(TileSourceFactory.tileSources.size)
        val MENU_OFFLINE: Int = getSafeMenuId()

        /**
         * @since 6.1.0
         */
        val MENU_SNAPSHOT: Int = getSafeMenuId()
        val MENU_STATES: Int = getSafeMenuId()

        val negate: FloatArray = floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f,  //red
            0f, -1.0f, 0f, 0f, 255f,  //green
            0f, 0f, -1.0f, 0f, 255f,  //blue
            0f, 0f, 0f, 1.0f, 0f //alpha
        )

        /**
         * provides a night mode like affect by inverting the map tile colors
         */
        val INVERT_COLORS: ColorFilter = ColorMatrixColorFilter(negate)
    }
}

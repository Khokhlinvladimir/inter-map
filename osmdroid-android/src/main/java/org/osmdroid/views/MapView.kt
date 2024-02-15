package org.osmdroid.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Scroller
import android.widget.ZoomButtonsController
import androidx.annotation.RequiresApi
import org.metalev.multitouch.controller.MultiTouchController
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas
import org.metalev.multitouch.controller.MultiTouchController.PointInfo
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale
import org.osmdroid.api.IGeoPoint
import org.osmdroid.api.IMapController
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.IStyledTileSource
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleInvalidationHandler
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.GeometryMath
import org.osmdroid.util.TileSystem
import org.osmdroid.util.TileSystemWebMercator
import org.osmdroid.views.overlay.DefaultOverlayManager
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayManager
import org.osmdroid.views.overlay.TilesOverlay
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ln
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.CUPCAKE)
open class MapView(
        context: Context,
        tileProvider: MapTileProviderBase?,
        tileRequestCompleteHandler: Handler?, attrs: AttributeSet?, hardwareAccelerated: Boolean
) : ViewGroup(context, attrs), IMapView, MultiTouchObjectCanvas<Any> {
    /**
     * Current zoom level for map tiles.
     */
    private var mZoomLevel = 0.0
    private var mTileProvider: MapTileProviderBase? = null
    private var mOverlayManager: OverlayManager? = null
    var mProjection: Projection? = null
    private var mMapOverlay: TilesOverlay? = null
    private var mGestureDetector: GestureDetector? = null
    /**
     * Handles map scrolling
     */
    private var mScroller: Scroller? = null
    var mIsFlinging = false
    /**
     * Set to true when the `Projection` actually adjusted the scroll values
     * Consequence: on this side effect, we must stop the flinging
     */
    private var mImpossibleFlinging = false
    val mIsAnimating = AtomicBoolean(false)
    var mMinimumZoomLevel: Double? = null
    var mMaximumZoomLevel: Double? = null
    private var mController: MapController? = null
    private var mZoomController: CustomZoomButtonsController? = null
    private var mMultiTouchController: MultiTouchController<Any>? = null
    /**
     * Initial pinch gesture pixel (typically, the middle of both fingers)
     */
    private val mMultiTouchScaleInitPoint = PointF()
    /**
     * Initial pinch gesture geo point, computed from [MapVie.mMultiTouchScaleInitPoint]
     * and the current Projection
     */
    private val mMultiTouchScaleGeoPoint = GeoPoint(0.0, 0.0)
    /**
     * Current pinch gesture pixel (again, the middle of both fingers)
     * We must ensure that this pixel is the projection of [MapView.mMultiTouchScaleGeoPoint]
     */
    private var mMultiTouchScaleCurrentPoint: PointF? = null
    // For rotation
    private var mapOrientation = 0f
    private val mInvalidateRect = Rect()
    private var mScrollableAreaLimitLatitude = false
    private var mScrollableAreaLimitNorth = 0.0
    private var mScrollableAreaLimitSouth = 0.0
    private var mScrollableAreaLimitLongitude = false
    private var mScrollableAreaLimitWest = 0.0
    private var mScrollableAreaLimitEast = 0.0
    private var mScrollableAreaLimitExtraPixelWidth = 0
    private var mScrollableAreaLimitExtraPixelHeight = 0
    private var mTileRequestCompleteHandler: Handler? = null
    private var mTilesScaledToDpi = false
    private var mTilesScaleFactor = 1f
    val mRotateScalePoint = Point()
    /* a point that will be reused to lay out added views */
    private val mLayoutPoint = Point()
    // Keep a set of listeners for when the maps have a layout
    private val mOnFirstLayoutListeners = LinkedList<OnFirstLayoutListener>()
    /* becomes true once onLayout has been called for the first time i.e. map is ready to go. */
    private var mLayoutOccurred = false
    private var horizontalMapRepetitionEnabled = true
    private var verticalMapRepetitionEnabled = true
    private var mCenter: GeoPoint? = null
    private var mMapScrollX: Long = 0
    private var mMapScrollY: Long = 0
    var mListners: ArrayList<MapListener> = ArrayList()
    private var mStartAnimationZoom = 0.0
    private var mZoomRounding = false
    private val mRepository = MapViewRepository(this)
    private val mRescaleScreenRect = Rect() // optimization
    private var mDestroyModeOnDetach = true
    /**
     * The map center used to be projected into the screen center.
     * Now we have a possible offset from the screen center; default offset is [0, 0].
     */
    private var mMapCenterOffsetX = 0
    private var mMapCenterOffsetY = 0
    private var enableFling = true
    private var pauseFling = false // issue 269, boolean used for disabling fling during zoom changes

    // ===========================================================
    // Constructor
    // ===========================================================

    constructor(context: Context,
                tileProvider: MapTileProviderBase?,
                tileRequestCompleteHandler: Handler?, attrs: AttributeSet?) : this(
            context, tileProvider, tileRequestCompleteHandler, attrs, Configuration.instance!!.isMapViewHardwareAccelerated
    )

    init {
        var tilesProvider: MapTileProviderBase? = tileProvider

        // Hacky workaround: If no storage location was set manually, we need to try to be
        // the first to give DefaultConfigurationProvider a chance to detect the best storage
        // location WITH a context. Otherwise there will be no valid cache directory on >API29!
        Configuration.instance!!.getOsmdroidTileCache(context)
        if (isInEditMode) {    //fix for edit mode in the IDE
            mTileRequestCompleteHandler = null
            mController = null
            mZoomController = null
            mScroller = null
            mGestureDetector = null
        }
        if (!hardwareAccelerated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        mController = MapController(this)
        mScroller = Scroller(context)
        if (tilesProvider == null) {
            val tileSource: ITileSource = getTileSourceFromAttributes(attrs)
            tilesProvider = MapTileProviderBasic(context.applicationContext, tileSource)
        }
        mTileRequestCompleteHandler = tileRequestCompleteHandler ?: SimpleInvalidationHandler(this)
        mTileProvider = tilesProvider
        mTileProvider!!.tileRequestCompleteHandlers.add(mTileRequestCompleteHandler)
        updateTileSizeForDensity(mTileProvider!!.tileSource)
        mMapOverlay = TilesOverlay(mTileProvider, context, horizontalMapRepetitionEnabled, verticalMapRepetitionEnabled)
        mOverlayManager = DefaultOverlayManager(mMapOverlay)
        mZoomController = CustomZoomButtonsController(this)
        mZoomController!!.setOnZoomListener(MapViewZoomListener())
        checkZoomButtons()
        mGestureDetector = GestureDetector(context, MapViewGestureDetectorListener())
        mGestureDetector!!.setOnDoubleTapListener(MapViewDoubleClickListener())
        if (Configuration.instance!!.isMapViewRecyclerFriendly) if (Build.VERSION.SDK_INT >= 16) setHasTransientState(true)
        mZoomController!!.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, null, null, attrs)
    constructor(context: Context) : this(context, null, null, null)
    constructor(context: Context, aTileProvider: MapTileProviderBase?) : this(context, aTileProvider, null)
    constructor(context: Context, aTileProvider: MapTileProviderBase?, tileRequestCompleteHandler: Handler?)
            : this(context, aTileProvider, tileRequestCompleteHandler, null)

// ===========================================================
    // Getter & Setter
    // ===========================================================

    override val controller: IMapController?
        get() = this.mController

    /**
     * You can add/remove/reorder your Overlays using the List of {@link Overlay}. The first (index
     * 0) Overlay gets drawn first, the one with the highest as the last one.
     */

    fun getOverlays(): MutableList<Overlay?>? {
        return getOverlayManager().overlays()
    }

    fun getOverlayManager(): OverlayManager {
        return mOverlayManager!!
    }

    fun setOverlayManager(overlayManager: OverlayManager) {
        mOverlayManager = overlayManager
    }

    fun getTileProvider(): MapTileProviderBase? {
        return this.mTileProvider
    }

    fun getScroller(): Scroller? {
        return mScroller
    }

    fun getTileRequestCompleteHandler(): Handler? {
        return mTileRequestCompleteHandler
    }

    override val latitudeSpanDouble: Double
        get() = this.getBoundingBox()!!.latitudeSpan

    override val longitudeSpanDouble: Double
        get() = this.getBoundingBox()!!.longitudeSpan

    fun getBoundingBox(): BoundingBox? {
        return projection.boundingBox
    }

    /**
     * Gets the current bounds of the screen in <I>screen coordinates</I>.
     */

    fun getScreenRect(reuse: Rect?): Rect {
        val out: Rect = getIntrinsicScreenRect(reuse)
        if (this.getMapOrientation() != 0f && this.getMapOrientation() != 180f) {
            GeometryMath.getBoundingBoxForRotatatedRectangle(out, out.centerX(), out.centerY(),
                    this.getMapOrientation(), out)
        }
        return out
    }

    fun getIntrinsicScreenRect(reuse: Rect?): Rect {
        val out = reuse ?: Rect()
        out[0, 0, width] = height
        return out
    }

    /**
     * Get a projection for converting between screen-pixel coordinates and latitude/longitude
     * coordinates. You should not hold on to this object for more than one draw, since the
     * projection of the map could change.
     *
     * @return The Projection of the map in its current state. You should not hold on to this object
     * for more than one draw, since the projection of the map could change.
     */

    override val projection: Projection
        get() {
            if (mProjection == null) {
                val localCopy = Projection(this)
                mProjection = localCopy
                localCopy.adjustOffsets(mMultiTouchScaleGeoPoint, mMultiTouchScaleCurrentPoint)
                if (mScrollableAreaLimitLatitude) {
                    localCopy.adjustOffsets(
                            mScrollableAreaLimitNorth, mScrollableAreaLimitSouth, true,
                            mScrollableAreaLimitExtraPixelHeight
                    )
                }
                if (mScrollableAreaLimitLongitude) {
                    localCopy.adjustOffsets(
                            mScrollableAreaLimitWest, mScrollableAreaLimitEast, false,
                            mScrollableAreaLimitExtraPixelWidth
                    )
                }
                mImpossibleFlinging = localCopy.setMapScroll(this)
            }
            return mProjection!!
        }

    /**
     * Use {@link #resetProjection()} instead
     *
     * @param p
     */

    @Deprecated("", ReplaceWith("mProjection = p"))
    fun setProjection(p: Projection) {
        mProjection = p
    }

    private fun resetProjection() {
        mProjection = null
    }

    /**
     * @deprecated use {@link IMapController#animateTo(IGeoPoint)} or {@link IMapController#setCenter(IGeoPoint)} instead
     */

    @Deprecated("use {@link IMapController#animateTo(IGeoPoint)} or {@link IMapController#setCenter(IGeoPoint)} instead", ReplaceWith("controller!!.animateTo(aCenter)"))
    fun setMapCenter(aCenter: IGeoPoint?) {
        controller!!.animateTo(aCenter)
    }

    /**
     * @deprecated use {@link #setMapCenter(IGeoPoint)}
     */

    @Deprecated("use {@link #setMapCenter(IGeoPoint)}", ReplaceWith("setMapCenter(GeoPoint(aLatitudeE6, aLongitudeE6))", "org.osmdroid.util.GeoPoint"))
    fun setMapCenter(aLatitudeE6: Int, aLongitudeE6: Int) {
        setMapCenter(GeoPoint(aLatitudeE6, aLongitudeE6))
    }


    @Deprecated("", ReplaceWith("setMapCenter(GeoPoint(aLatitude, aLongitude))", "org.osmdroid.util.GeoPoint"))
    fun setMapCenter(aLatitude: Double, aLongitude: Double) {
        setMapCenter(GeoPoint(aLatitude, aLongitude))
    }

    fun isTilesScaledToDpi(): Boolean {
        return mTilesScaledToDpi
    }

    /**
     * if true, tiles are scaled to the current DPI of the display. This effectively
     * makes it easier to read labels, how it may appear pixelated depending on the map
     * source.<br></br>
     * if false, tiles are rendered in their real size
     *
     * @param tilesScaledToDpi
     */
    fun setTilesScaledToDpi(tilesScaledToDpi: Boolean) {
        mTilesScaledToDpi = tilesScaledToDpi
        updateTileSizeForDensity(getTileProvider()!!.tileSource)
    }

    fun getTilesScaleFactor(): Float {
        return mTilesScaleFactor
    }


    /**
     * Setting an additional scale factor both for ScaledToDpi and standard size
     * > 1.0 enlarges map display, < 1.0 shrinks map display
     */
    fun setTilesScaleFactor(pTilesScaleFactor: Float) {
        mTilesScaleFactor = pTilesScaleFactor
        updateTileSizeForDensity(getTileProvider()!!.tileSource)
    }

    fun resetTilesScaleFactor() {
        mTilesScaleFactor = 1f
        updateTileSizeForDensity(getTileProvider()!!.tileSource)
    }

    private fun updateTileSizeForDensity(aTileSource: ITileSource) {
        val tileSize = aTileSource.tileSizePixels
        val density = resources.displayMetrics.density * 256 / tileSize
        val size = (tileSize * if (isTilesScaledToDpi()) density * mTilesScaleFactor else mTilesScaleFactor).toInt()
        if (Configuration.instance!!.isDebugMapView) Log.d(IMapView.LOGTAG, "Scaling tiles to $size")
        TileSystem.setTileSize(size)
    }

    fun setTileSource(aTileSource: ITileSource) {
        this.mTileProvider!!.tileSource = aTileSource
        updateTileSizeForDensity(aTileSource)
        this.checkZoomButtons()
        this.setZoomLevel(mZoomLevel) // revalidate zoom level
        postInvalidate()
    }

    /**
     * @param aZoomLevel the zoom level bound by the tile source
     *                   Used to be an int - is a double since 6.0
     */

    fun setZoomLevel(aZoomLevel: Double): Double {
        val newZoomLevel = getMinZoomLevel().coerceAtLeast(maxZoomLevel.coerceAtMost(aZoomLevel))
        val curZoomLevel = mZoomLevel

        if (newZoomLevel != curZoomLevel) {
            if (mScroller != null) //fix for edit mode in the IDE
                mScroller!!.forceFinished(true)
            mIsFlinging = false
        }

        // Get our current center point
        val centerGeoPoint: IGeoPoint = projection.currentCenter
        mZoomLevel = newZoomLevel

        setExpectedCenter(centerGeoPoint)
        this.checkZoomButtons()

        if (isLayoutOccurred()) {
            controller!!.setCenter(centerGeoPoint)

            // snap for all snappables
            val snapPoint = Point()
            val pj: Projection = projection
            if (getOverlayManager().onSnapToItem(mMultiTouchScaleInitPoint.x.toInt(), mMultiTouchScaleInitPoint.y.toInt(), snapPoint, this)) {
                val geoPoint = pj.fromPixels(snapPoint.x, snapPoint.y, null, false)
                controller!!.animateTo(geoPoint)
            }
            this.mTileProvider!!.rescaleCache(pj, newZoomLevel, curZoomLevel, getScreenRect(mRescaleScreenRect))
            pauseFling = true // issue 269, pause fling during zoom changes
        }

        // do callback on listener
        if (newZoomLevel != curZoomLevel) {
            var event: ZoomEvent? = null
            for (mapListener in mListners) mapListener.onZoom(event ?: ZoomEvent(this, newZoomLevel).also { event = it })
        }
        requestLayout() // Allows any views fixed to a Location in the MapView to adjust
        invalidate()
        return mZoomLevel
    }

    /**
     * Zoom the map to enclose the specified bounding box, as closely as possible. Must be called
     * after display layout is complete, or screen dimensions are not known, and will always zoom to
     * center of zoom level 0.<br></br>
     * Suggestion: Check getIntrinsicScreenRect(null).getHeight() &gt; 0
     */
    fun zoomToBoundingBox(boundingBox: BoundingBox?, animated: Boolean) {
        zoomToBoundingBox(boundingBox!!, animated, 0)
    }

    /**
     * @param pBoundingBox        Bounding box we want to zoom to; may be a single [GeoPoint]
     * @param pAnimated           Animation or immediate action?
     * @param pBorderSizeInPixels Border size around the bounding box
     * @param pMaximumZoom        Maximum zoom we want from bounding box computation
     * @param pAnimationSpeed     Animation duration, in milliseconds
     */
    fun zoomToBoundingBox(
            pBoundingBox: BoundingBox,
            pAnimated: Boolean,
            pBorderSizeInPixels: Int,
            pMaximumZoom: Double,
            pAnimationSpeed: Long?
    ): Double {
        var nextZoom = mTileSystem.getBoundingBoxZoom(
                pBoundingBox,
                width - 2 * pBorderSizeInPixels,
                height - 2 * pBorderSizeInPixels
        )
        if (nextZoom == Double.MIN_VALUE // e.g. single point bounding box
                || nextZoom > pMaximumZoom) { // e.g. tiny bounding box
            nextZoom = pMaximumZoom
        }
        nextZoom = maxZoomLevel.coerceAtMost(nextZoom.coerceAtLeast(getMinZoomLevel()))
        val center = pBoundingBox.centerWithDateLine

        val projection = Projection(
                nextZoom,
                width,
                height,
                center,
                getMapOrientation(),
                isHorizontalMapRepetitionEnabled(),
                isVerticalMapRepetitionEnabled(),
                getMapCenterOffsetX(),
                getMapCenterOffsetY()
        )
        val point = Point()
        val longitude = pBoundingBox.centerLongitude
        projection.toPixels(GeoPoint(pBoundingBox.actualNorth, longitude), point)
        val north = point.y
        projection.toPixels(GeoPoint(pBoundingBox.actualSouth, longitude), point)
        val south = point.y
        val offset = (height - south - north) / 2
        if (offset != 0) {
            projection.adjustOffsets(0, offset.toLong())
            projection.fromPixels(width / 2, height / 2, center)
        }
        if (pAnimated) {
            controller!!.animateTo(center, nextZoom, pAnimationSpeed)
        } else { // it's best to set the zoom first, so that the center is accurate
            controller!!.setZoom(nextZoom)
            controller!!.setCenter(center)
        }
        return nextZoom
    }

    fun zoomToBoundingBox(pBoundingBox: BoundingBox?, pAnimated: Boolean, pBorderSizeInPixels: Int) {
        zoomToBoundingBox(pBoundingBox!!, pAnimated, pBorderSizeInPixels, maxZoomLevel, null)
    }

    /**
     * Get the current ZoomLevel for the map tiles.
     *
     * @return the current ZoomLevel between 0 (equator) and 18/19(closest), depending on the tile
     * source chosen.
     */

    @Deprecated("", ReplaceWith("getZoomLevelDouble().toInt()"))
    override val zoomLevel: Int
        get() = zoomLevelDouble.toInt()


    override val zoomLevelDouble: Double
        get() = mZoomLevel

    /**
     * Get the current ZoomLevel for the map tiles.
     *
     * @param aPending if true and we're animating then return the zoom level that we're animating
     * towards, otherwise return the current zoom level
     * Used to be an int - is a double since 6.0
     * @return the zoom level
     */
    @Deprecated("", ReplaceWith("getZoomLevelDouble()"))
    fun getZoomLevel(aPending: Boolean): Double {
        return zoomLevelDouble
    }

    /**
     * Get the minimum allowed zoom level for the maps.
     */

    fun getMinZoomLevel(): Double {
        return if (mMinimumZoomLevel == null) mMapOverlay!!.minimumZoomLevel.toDouble() else mMinimumZoomLevel!!
    }

    /**
     * Get the maximum allowed zoom level for the maps.
     */

    override val maxZoomLevel: Double
        get() = if (mMaximumZoomLevel == null) mMapOverlay!!.maximumZoomLevel.toDouble() else mMaximumZoomLevel!!

    /**
     * Set the minimum allowed zoom level, or pass null to use the minimum zoom level from the tile
     * provider.
     */
    fun setMinZoomLevel(zoomLevel: Double) {
        mMinimumZoomLevel = zoomLevel
    }

    /**
     * Set the maximum allowed zoom level, or pass null to use the maximum zoom level from the tile
     * provider.
     */
    fun setMaxZoomLevel(zoomLevel: Double) {
        mMaximumZoomLevel = zoomLevel
    }

    fun canZoomIn(): Boolean {
        return mZoomLevel < maxZoomLevel
    }

    fun canZoomOut(): Boolean {
        return mZoomLevel > getMinZoomLevel()
    }


    /**
     * Zoom in by one zoom level.
     * Use [MapController.zoomIn]} instead
     */
    @Deprecated("", ReplaceWith("controller!!.zoomIn()"))
    fun zoomIn(): Boolean {
        return controller!!.zoomIn()
    }

    @Deprecated("")
    fun zoomInFixing(point: IGeoPoint?): Boolean {
        val coords: Point? = projection.toPixels(point, null)
        return controller!!.zoomInFixing(coords!!.x, coords.y)
    }

    @Deprecated("", ReplaceWith("getController().zoomInFixing(xPixel, yPixel)"))
    fun zoomInFixing(xPixel: Int, yPixel: Int): Boolean {
        return controller!!.zoomInFixing(xPixel, yPixel)
    }

    /**
     * Zoom out by one zoom level.
     * Use [MapController.zoomOut] instead
     */
    @Deprecated("", ReplaceWith("controller!!.zoomOut()"))
    fun zoomOut(): Boolean {
        return controller!!.zoomOut()
    }

    @Deprecated("")
    fun zoomOutFixing(point: IGeoPoint): Boolean {
        val coords: Point? = projection.toPixels(point, null)
        return zoomOutFixing(coords!!.x, coords.y)
    }

    @Deprecated("", ReplaceWith("controller!!.zoomOutFixing(xPixel, yPixel)"))
    fun zoomOutFixing(xPixel: Int, yPixel: Int): Boolean {
        return controller!!.zoomOutFixing(xPixel, yPixel)
    }

    /**
     * Returns the current center-point position of the map, as a GeoPoint (latitude and longitude).
     * <br><br>
     * Gives you the actual current map center, as the Projection computes it from the middle of
     * the screen. Most of the time it's supposed to be approximately the same value (because
     * of computing rounding side effects), but in some cases (current zoom gesture or scroll
     * limits) the values may differ (typically, when {@link Projection#adjustOffsets} had to fine-tune
     * the map center).
     *
     * @return A GeoPoint of the map's center-point.
     * @see #getExpectedCenter()
     */


    fun getMapCenter(pReuse: GeoPoint?): IGeoPoint? {
        return projection.fromPixels(width / 2, height / 2, pReuse, false)
    }

    override val mapCenter: IGeoPoint?
        get() = getMapCenter(null)

    /**
     * rotates the map to the desired heading
     *
     * @param degrees
     */
    fun setMapOrientation(degrees: Float) {
        setMapOrientation(degrees, true)
    }

    /**
     * There are some cases when we don't need explicit redraw
     */
    fun setMapOrientation(degrees: Float, forceRedraw: Boolean) {
        mapOrientation = degrees % 360.0f
        if (forceRedraw) {
            requestLayout() // Allows any views fixed to a Location in the MapView to adjust
            invalidate()
        }
    }

    fun getMapOrientation(): Float {
        return mapOrientation
    }

    @Deprecated("", ReplaceWith("1f"))
    fun getMapScale(): Float {
        return 1f
    }

    /**
     * Whether to use the network connection if it's available.
     */
    fun useDataConnection(): Boolean {
        return mMapOverlay!!.useDataConnection()
    }

    /**
     * Set whether to use the network connection if it's available.
     *
     * @param aMode if true use the network connection if it's available. if false don't use the
     * network connection even if it's available.
     */
    fun setUseDataConnection(aMode: Boolean) {
        mMapOverlay!!.setUseDataConnection(aMode)
    }

    /**
     * Set the map to limit it's scrollable view to the specified BoundingBox. Note this does not
     * limit zooming so it will be possible for the user to zoom out to an area that is larger than the
     * limited area.
     *
     * @param boundingBox A lat/long bounding box to limit scrolling to, or null to remove any scrolling
     * limitations
     */
    fun setScrollableAreaLimitDouble(boundingBox: BoundingBox?) {
        if (boundingBox == null) {
            resetScrollableAreaLimitLatitude()
            resetScrollableAreaLimitLongitude()
        } else {
            setScrollableAreaLimitLatitude(boundingBox.actualNorth, boundingBox.actualSouth, 0)
            setScrollableAreaLimitLongitude(boundingBox.lonWest, boundingBox.lonEast, 0)
        }
    }

    fun resetScrollableAreaLimitLatitude() {
        mScrollableAreaLimitLatitude = false
    }

    fun resetScrollableAreaLimitLongitude() {
        mScrollableAreaLimitLongitude = false
    }

    /**
     * sets the scroll limit
     * Example:
     * To block vertical scroll of the view outside north/south poles:
     * mapView.setScrollableAreaLimitLatitude(MapView.getTileSystem().getMaxLatitude(),
     * MapView.getTileSystem().getMinLatitude(),
     * 0);
     * Warning:
     * Don't use latitude values outside the [MapView.getTileSystem().getMinLatitude(),
     * MapView.getTileSystem().getMaxLatitude()] range, this would cause an ANR.
     *
     * @param pNorth            decimal degrees latitude
     * @param pSouth            decimal degrees latitude
     * @param pExtraPixelHeight in pixels, enables scrolling this many pixels past the bounds
     */

    fun setScrollableAreaLimitLatitude(
            pNorth: Double,
            pSouth: Double,
            pExtraPixelHeight: Int
    ) {
        mScrollableAreaLimitLatitude = true
        mScrollableAreaLimitNorth = pNorth
        mScrollableAreaLimitSouth = pSouth
        mScrollableAreaLimitExtraPixelHeight = pExtraPixelHeight
    }

    /**
     * sets the scroll limit
     *
     * @param pWest            decimal degrees longitude
     * @param pEast            decimal degrees longitude
     * @param pExtraPixelWidth in pixels, enables scrolling this many pixels past the bounds
     */
    fun setScrollableAreaLimitLongitude(
            pWest: Double,
            pEast: Double,
            pExtraPixelWidth: Int
    ) {
        mScrollableAreaLimitLongitude = true
        mScrollableAreaLimitWest = pWest
        mScrollableAreaLimitEast = pEast
        mScrollableAreaLimitExtraPixelWidth = pExtraPixelWidth
    }

    fun isScrollableAreaLimitLatitude(): Boolean {
        return mScrollableAreaLimitLatitude
    }

    fun isScrollableAreaLimitLongitude(): Boolean {
        return mScrollableAreaLimitLongitude
    }

    fun invalidateMapCoordinates(dirty: Rect) {
        invalidateMapCoordinates(dirty.left, dirty.top, dirty.right, dirty.bottom, false)
    }

    fun invalidateMapCoordinates(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
    ) {
        invalidateMapCoordinates(left, top, right, bottom, false)
    }

    fun postInvalidateMapCoordinates(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
    ) {
        invalidateMapCoordinates(left, top, right, bottom, true)
    }

    private fun invalidateMapCoordinates(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            post: Boolean
    ) {
        mInvalidateRect[left, top, right] = bottom
        val centerX = width / 2
        val centerY = height / 2
        if (this.getMapOrientation() != 0f)
            GeometryMath.getBoundingBoxForRotatatedRectangle(mInvalidateRect, centerX, centerY,
                    this.getMapOrientation() + 180, mInvalidateRect)

        if (post) super.postInvalidate(mInvalidateRect.left, mInvalidateRect.top, mInvalidateRect.right, mInvalidateRect.bottom)
        else super.invalidate(mInvalidateRect)
    }

    /**
     * Returns a set of layout parameters with a width of
     * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}, a height of
     * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT} at the {@link GeoPoint} (0, 0) align
     * with {@link MapView.LayoutParams#BOTTOM_CENTER}.
     */


    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                null,
                MapView.LayoutParams.BOTTOM_CENTER,
                0,
                0
        )
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    // Override to allow type-checking of LayoutParams.
    fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Get the children to measure themselves so we know their size in onLayout()
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(
            changed: Boolean,
            l: Int,
            t: Int,
            r: Int,
            b: Int
    ) {
        myOnLayout(changed, l, t, r, b)
    }

    /**
     * Code was moved from [.onLayout]
     * in order to avoid Android Studio warnings on direct calls
     */
    private fun myOnLayout(
            changed: Boolean,
            l: Int,
            t: Int,
            r: Int,
            b: Int
    ) {
        resetProjection()
        val count = childCount

        for (i in 0 until count) {
            val child = getChildAt(i)

            if (child.visibility != GONE) {

                val lp = child.layoutParams as LayoutParams
                val childHeight = child.measuredHeight
                val childWidth = child.measuredWidth
                projection.toPixels(lp.geoPoint, mLayoutPoint)
                // Apply rotation of mLayoutPoint around the center of the map
                if (getMapOrientation() != 0f) {
                    val p: Point = projection.rotateAndScalePoint(mLayoutPoint.x, mLayoutPoint.y, null)
                    mLayoutPoint.x = p.x
                    mLayoutPoint.y = p.y
                }
                val x = mLayoutPoint.x.toLong()
                val y = mLayoutPoint.y.toLong()
                var childLeft = x
                var childTop = y

                when (lp.alignment) {
                    LayoutParams.TOP_LEFT -> {
                        childLeft = paddingLeft + x
                        childTop = paddingTop + y
                    }

                    LayoutParams.TOP_CENTER -> {
                        childLeft = paddingLeft + x - childWidth / 2
                        childTop = paddingTop + y
                    }

                    LayoutParams.TOP_RIGHT -> {
                        childLeft = paddingLeft + x - childWidth
                        childTop = paddingTop + y
                    }

                    LayoutParams.CENTER_LEFT -> {
                        childLeft = paddingLeft + x
                        childTop = paddingTop + y - childHeight / 2
                    }

                    LayoutParams.CENTER -> {
                        childLeft = paddingLeft + x - childWidth / 2
                        childTop = paddingTop + y - childHeight / 2
                    }

                    LayoutParams.CENTER_RIGHT -> {
                        childLeft = paddingLeft + x - childWidth
                        childTop = paddingTop + y - childHeight / 2
                    }

                    MapView.LayoutParams.BOTTOM_LEFT -> {
                        childLeft = paddingLeft + x
                        childTop = paddingTop + y - childHeight
                    }

                    MapView.LayoutParams.BOTTOM_CENTER -> {
                        childLeft = paddingLeft + x - childWidth / 2
                        childTop = paddingTop + y - childHeight
                    }

                    MapView.LayoutParams.BOTTOM_RIGHT -> {
                        childLeft = paddingLeft + x - childWidth
                        childTop = paddingTop + y - childHeight
                    }
                }
                childLeft += lp.offsetX.toLong()
                childTop += lp.offsetY.toLong()
                child.layout(
                        TileSystem.truncateToInt(childLeft),
                        TileSystem.truncateToInt(childTop),
                        TileSystem.truncateToInt(childLeft + childWidth),
                        TileSystem.truncateToInt(childTop + childHeight)
                )
            }
        }
        if (!isLayoutOccurred()) {
            mLayoutOccurred = true
            for (listener in mOnFirstLayoutListeners) listener.onFirstLayout(this, l, t, r, b)
            mOnFirstLayoutListeners.clear()
        }
        resetProjection()
    }

    /**
     * enables you to add a listener for when the map is ready to go.
     * @param listener
     */

    fun addOnFirstLayoutListener(listener: OnFirstLayoutListener?) {
        // Don't add if we already have a layout
        if (!isLayoutOccurred()) mOnFirstLayoutListeners.add(listener!!)
    }

    fun removeOnFirstLayoutListener(listener: OnFirstLayoutListener?) {
        mOnFirstLayoutListeners.remove(listener)
    }

    fun isLayoutOccurred(): Boolean {
        return mLayoutOccurred
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    /**
     * activities/fragments using osmdroid should call this to release resources, pause gps, sensors, timers, etc
     */
    fun onPause() {
        getOverlayManager().onPause()
    }

    /**
     * activities/fragments using osmdroid should call this to release resources, pause gps, sensors, timers, etc
     */
    fun onResume() {
        getOverlayManager().onResume()
    }

    /**
     * destroys the map view, all references to listeners, all overlays, etc
     */
    fun onDetach() {
        getOverlayManager().onDetach(this)
        this.mTileProvider!!.detach()
        if (mZoomController != null) {
            mZoomController!!.onDetach()
        }
        if (mTileRequestCompleteHandler is SimpleInvalidationHandler) {
            (mTileRequestCompleteHandler as SimpleInvalidationHandler).destroy()
        }
        mTileRequestCompleteHandler = null
        if (mProjection != null) mProjection!!.detach()
        mProjection = null
        mRepository.onDetach()
        mListners.clear()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val result = getOverlayManager().onKeyDown(keyCode, event, this)
        return result || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val result = getOverlayManager().onKeyUp(keyCode, event, this)
        return result || super.onKeyUp(keyCode, event)
    }

    override fun onTrackballEvent(event: MotionEvent): Boolean {
        if (getOverlayManager().onTrackballEvent(event, this)) {
            return true
        }
        scrollBy((event.x * 25).toInt(), (event.y * 25).toInt())
        return super.onTrackballEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (Configuration.instance!!.isDebugMapView) {
            Log.d(IMapView.LOGTAG, "dispatchTouchEvent($event)")
        }
        if (mZoomController!!.isTouched(event)) {
            mZoomController!!.activate()
            return true
        }

        // Get rotated event for some touch listeners.
        val rotatedEvent = rotateTouchEvent(event)
        try {
            if (super.dispatchTouchEvent(event)) {
                if (Configuration.instance!!.isDebugMapView) {
                    Log.d(IMapView.LOGTAG, "super handled onTouchEvent")
                }
                return true
            }

            if (getOverlayManager().onTouchEvent(rotatedEvent, this)) {
                return true
            }

            var handled = false
            if (mMultiTouchController != null && mMultiTouchController!!.onTouchEvent(event)) {
                if (Configuration.instance!!.isDebugMapView) {
                    Log.d(IMapView.LOGTAG, "mMultiTouchController handled onTouchEvent")
                }
                handled = true
            }

            if (mGestureDetector!!.onTouchEvent(rotatedEvent)) {
                if (Configuration.instance!!.isDebugMapView) {
                    Log.d(IMapView.LOGTAG, "mGestureDetector handled onTouchEvent")
                }
                handled = true
            }

            if (handled) return true

        } finally {
            if (rotatedEvent != event) rotatedEvent.recycle()
        }
        if (Configuration.instance!!.isDebugMapView) {
            Log.d(IMapView.LOGTAG, "no-one handled onTouchEvent")
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    private fun rotateTouchEvent(ev: MotionEvent): MotionEvent {

        if (getMapOrientation() == 0f) return ev

        val rotatedEvent = MotionEvent.obtain(ev)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            projection.unrotateAndScalePoint(ev.x.toInt(), ev.y.toInt(),
                    mRotateScalePoint)
            rotatedEvent.setLocation(mRotateScalePoint.x.toFloat(), mRotateScalePoint.y.toFloat())
        } else {
            // This method is preferred since it will rotate historical touch events too
            rotatedEvent.transform(projection.invertedScaleRotateCanvasMatrix)
        }
        return rotatedEvent
    }

    override fun computeScroll() {
        if (mScroller == null) { //fix for edit mode in the IDE
            return
        }
        if (!mIsFlinging) {
            return
        }
        if (!mScroller!!.computeScrollOffset()) {
            return
        }
        if (mScroller!!.isFinished) {
            // we deliberately ignore the very last scrollTo, which sometimes provokes map hiccups
            mIsFlinging = false
        } else {
            scrollTo(mScroller!!.currX, mScroller!!.currY)
            postInvalidate()
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        setMapScroll(x.toLong(), y.toLong())
        resetProjection()
        invalidate()

        // Force a layout, so that children are correctly positioned according to map orientation
        if (getMapOrientation() != 0f) myOnLayout(true, left, top, right, bottom)

        // do callback on listener
        var event: ScrollEvent? = null
        for (mapListener in mListners) {
            mapListener.onScroll(event ?: ScrollEvent(this, x, y).also { event = it })
        }
    }


    override fun scrollBy(x: Int, y: Int) {
        scrollTo((getMapScrollX() + x).toInt(), (getMapScrollY() + y).toInt())
    }

    override fun setBackgroundColor(color: Int) {
        mMapOverlay!!.loadingBackgroundColor = color
        invalidate()
    }

    override fun dispatchDraw(c: Canvas) {
        val startMs = System.currentTimeMillis()

        // Reset the projection
        resetProjection()

        // Apply the scale and rotate operations
        projection.save(c, true, false)

        /* Draw background */
        // c.drawColor(mBackgroundColor);
        try {
            /* Draw all Overlays. */
            getOverlayManager().onDraw(c, this)
            // Restore the canvas matrix
            projection.restore(c, false)
            if (mZoomController != null) {
                mZoomController!!.draw(c)
            }
            super.dispatchDraw(c)
        } catch (ex: Exception) {
            //for edit mode
            Log.e(IMapView.LOGTAG, "error dispatchDraw, probably in edit mode", ex)
        }
        if (Configuration.instance!!.isDebugMapView) {
            val endMs = System.currentTimeMillis()
            Log.d(IMapView.LOGTAG, "Rendering overall: " + (endMs - startMs) + "ms")
        }
    }

    override fun onDetachedFromWindow() {
        if (mDestroyModeOnDetach) {
            onDetach()
        }
        super.onDetachedFromWindow()
    }


    // ===========================================================
    // Animation
    // ===========================================================
    /**
     * Determines if maps are animating a zoom operation. Useful for overlays to avoid recalculating
     * during an animation sequence.
     *
     * @return boolean indicating whether view is animating.
     */
    fun isAnimating(): Boolean {
        return mIsAnimating.get()
    }

    // ===========================================================
    // Implementation of MultiTouchObjectCanvas
    // ===========================================================
    override fun getDraggableObjectAtPoint(pt: PointInfo): Any? {
        return if (isAnimating()) {
            // Zoom animations use the mMultiTouchScale variables to perform their animations so we
            // don't want to step on that.
            null
        } else {
            setMultiTouchScaleInitPoint(pt.x, pt.y)
            this
        }
    }

    override fun getPositionAndScale(obj: Any?, objPosAndScaleOut: PositionAndScale) {
        startAnimation()
        objPosAndScaleOut[mMultiTouchScaleInitPoint.x, mMultiTouchScaleInitPoint.y, true, 1f, false, 0f, 0f, false] = 0f
    }

    override fun selectObject(obj: Any?, pt: PointInfo?) {
        if (mZoomRounding) {
            mZoomLevel = mZoomLevel.roundToInt().toDouble()
            invalidate()
        }
        resetMultiTouchScale()
    }

    override fun setPositionAndScale(
            obj: Any?,
            aNewObjPosAndScale: PositionAndScale,
            aTouchPoint: PointInfo?
    ): Boolean {
        setMultiTouchScaleCurrentPoint(aNewObjPosAndScale.xOff, aNewObjPosAndScale.yOff)
        setMultiTouchScale(aNewObjPosAndScale.scale)
        requestLayout() // Allows any views fixed to a Location in the MapView to adjust
        invalidate()
        return true
    }

    fun resetMultiTouchScale() {
        mMultiTouchScaleCurrentPoint = null
    }

    fun setMultiTouchScaleInitPoint(pX: Float, pY: Float) {
        mMultiTouchScaleInitPoint[pX] = pY
        val unRotatedPixel: Point = projection.unrotateAndScalePoint(pX.toInt(), pY.toInt(), null)
        projection.fromPixels(unRotatedPixel.x, unRotatedPixel.y, mMultiTouchScaleGeoPoint)
        setMultiTouchScaleCurrentPoint(pX, pY)
    }


    fun setMultiTouchScaleCurrentPoint(pX: Float, pY: Float) {
        mMultiTouchScaleCurrentPoint = PointF(pX, pY)
    }

    fun setMultiTouchScale(pMultiTouchScale: Float) {
        setZoomLevel(ln(pMultiTouchScale.toDouble()) / ln(2.0) + mStartAnimationZoom)
    }

    fun startAnimation() {
        mStartAnimationZoom = zoomLevelDouble
    }

    /*
     * Set the MapListener for this view
     * @deprecated use addMapListener instead
     */
    @Deprecated("", ReplaceWith("mListners.add(ml!!)"))
    fun setMapListener(ml: MapListener?) {
        mListners.add(ml!!)
    }

    /**
     * Just like the old setMapListener, except it supports more than one
     *
     * @param mapListener
     */
    fun addMapListener(mapListener: MapListener?) {
        mListners.add(mapListener!!)
    }

    /**
     * Removes a map listener
     *
     * @param mapListener
     */
    fun removeMapListener(mapListener: MapListener?) {
        mListners.remove(mapListener)
    }

    // ===========================================================
    // Methods
    // ===========================================================
    private fun checkZoomButtons() {
        mZoomController!!.setZoomInEnabled(canZoomIn())
        mZoomController!!.setZoomOutEnabled(canZoomOut())
    }

    /**
     * @deprecated Use {@link #getZoomController().setVisibility()} instead
     */

    @Deprecated("")
    fun setBuiltInZoomControls(on: Boolean) {
        mZoomController!!.setVisibility(
                if (on) CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
                else CustomZoomButtonsController.Visibility.NEVER
        )
    }

    fun setMultiTouchControls(on: Boolean) {
        mMultiTouchController = if (on) MultiTouchController(this, false) else null
    }

    fun isHorizontalMapRepetitionEnabled(): Boolean {
        return horizontalMapRepetitionEnabled
    }

    /**
     * If horizontalMapRepetition is enabled the map repeats in left/right direction and scrolling wraps around the
     * edges. If disabled the map is only shown once for the horizontal direction. Default is true.
     *
     * @param horizontalMapRepetitionEnabled
     */
    fun setHorizontalMapRepetitionEnabled(horizontalMapRepetitionEnabled: Boolean) {
        this.horizontalMapRepetitionEnabled = horizontalMapRepetitionEnabled
        mMapOverlay!!.isHorizontalWrapEnabled = horizontalMapRepetitionEnabled
        resetProjection()
        this.invalidate()
    }

    fun isVerticalMapRepetitionEnabled(): Boolean {
        return verticalMapRepetitionEnabled
    }

    /**
     * If verticalMapRepetition is enabled the map repeats in top/bottom direction and scrolling wraps around the
     * edges. If disabled the map is only shown once for the vertical direction. Default is true.
     *
     * @param verticalMapRepetitionEnabled
     */
    fun setVerticalMapRepetitionEnabled(verticalMapRepetitionEnabled: Boolean) {
        this.verticalMapRepetitionEnabled = verticalMapRepetitionEnabled
        mMapOverlay!!.isVerticalWrapEnabled = verticalMapRepetitionEnabled
        resetProjection()
        this.invalidate()
    }

    private fun getTileSourceFromAttributes(aAttributeSet: AttributeSet?): ITileSource {

        var tileSource: ITileSource = TileSourceFactory.DEFAULT_TILE_SOURCE

        if (aAttributeSet != null) {
            val tileSourceAttr = aAttributeSet.getAttributeValue(null, "tilesource")
            if (tileSourceAttr != null) {
                try {
                    val r = TileSourceFactory.getTileSource(tileSourceAttr)
                    Log.i(IMapView.LOGTAG, "Using tile source specified in layout attributes: $r")
                    tileSource = r
                } catch (e: IllegalArgumentException) {
                    Log.w(IMapView.LOGTAG, "Invalid tile source specified in layout attributes: $tileSource")
                }
            }
        }
        if (aAttributeSet != null && tileSource is IStyledTileSource<*>) {
            val style = aAttributeSet.getAttributeValue(null, "style")
            if (style == null) {
                Log.i(IMapView.LOGTAG, "Using default style: 1")
            } else {
                Log.i(IMapView.LOGTAG, "Using style specified in layout attributes: $style")
                (tileSource as IStyledTileSource<*>).setStyle(style)
            }
        }
        Log.i(IMapView.LOGTAG, "Using tile source: " + tileSource.name())
        return tileSource
    }


    fun setFlingEnabled(b: Boolean) {
        enableFling = b
    }

    fun isFlingEnabled(): Boolean {
        return enableFling
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    inner class MapViewGestureDetectorListener : GestureDetector.OnGestureListener {
        override fun onDown(e: MotionEvent): Boolean {

            // Stop scrolling if we are in the middle of a fling!
            if (mIsFlinging) {
                mScroller?.abortAnimation()
                mIsFlinging = false
            }
            if (this@MapView.getOverlayManager().onDown(e, this@MapView)) {
                return true
            }
            mZoomController?.activate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent,
                             velocityX: Float, velocityY: Float): Boolean {
            if (!enableFling || pauseFling) {
                // issue 269, if fling occurs during zoom changes, pauseFling is equals to true, so fling is canceled. But need to reactivate fling for next time.
                pauseFling = false
                return false
            }
            if (this@MapView.getOverlayManager()
                            .onFling(e1, e2, velocityX, velocityY, this@MapView)) {
                return true
            }
            if (mImpossibleFlinging) {
                mImpossibleFlinging = false
                return false
            }
            mIsFlinging = true
            mScroller?.fling(getMapScrollX().toInt(), getMapScrollY().toInt(), -velocityX.toInt(), -velocityY.toInt(), Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (mMultiTouchController != null && mMultiTouchController!!.isPinching()) {
                return
            }
            this@MapView.getOverlayManager().onLongPress(e, this@MapView)
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float,
                              distanceY: Float): Boolean {
            if (this@MapView.getOverlayManager().onScroll(e1, e2, distanceX, distanceY,
                            this@MapView)) {
                return true
            }
            scrollBy(distanceX.toInt(), distanceY.toInt())
            return true
        }

        override fun onShowPress(e: MotionEvent) {
            this@MapView.getOverlayManager().onShowPress(e, this@MapView)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return this@MapView.getOverlayManager().onSingleTapUp(e, this@MapView)
        }
    }

    inner class MapViewDoubleClickListener : GestureDetector.OnDoubleTapListener {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (this@MapView.getOverlayManager().onDoubleTap(e, this@MapView)) {
                return true
            }
            projection.rotateAndScalePoint(e.x.toInt(), e.y.toInt(), mRotateScalePoint)
            return controller!!.zoomInFixing(mRotateScalePoint.x, mRotateScalePoint.y)
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            return this@MapView.getOverlayManager().onDoubleTapEvent(e, this@MapView)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return this@MapView.getOverlayManager().onSingleTapConfirmed(e, this@MapView)
        }
    }

    @RequiresApi(Build.VERSION_CODES.DONUT)
    inner class MapViewZoomListener : CustomZoomButtonsController.OnZoomListener, ZoomButtonsController.OnZoomListener {
        override fun onZoom(zoomIn: Boolean) {
            if (zoomIn) {
                controller!!.zoomIn()
            } else {
                controller!!.zoomOut()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {}
    }

    // ===========================================================
    // Public Classes
    // ===========================================================
    /**
     * Per-child layout information associated with OpenStreetMapView.
     */
    class LayoutParams : ViewGroup.LayoutParams {
        /**
         * The location of the child within the map view.
         */
        var geoPoint: IGeoPoint? = null

        /**
         * The alignment the alignment of the view compared to the location.
         */
        var alignment = 0
        var offsetX = 0
        var offsetY = 0

        /**
         * Creates a new set of layout parameters with the specified width, height and location.
         *
         * @param width     the width, either [.FILL_PARENT], [.WRAP_CONTENT] or a fixed size
         * in pixels
         * @param height    the height, either [.FILL_PARENT], [.WRAP_CONTENT] or a fixed size
         * in pixels
         * @param geoPoint  the location of the child within the map view
         * @param alignment the alignment of the view compared to the location [.BOTTOM_CENTER],
         * [.BOTTOM_LEFT], [.BOTTOM_RIGHT] [.TOP_CENTER],
         * [.TOP_LEFT], [.TOP_RIGHT]
         * @param offsetX   the additional X offset from the alignment location to draw the child within
         * the map view
         * @param offsetY   the additional Y offset from the alignment location to draw the child within
         * the map view
         */
        constructor(
                width: Int,
                height: Int,
                geoPoint: IGeoPoint?,
                alignment: Int,
                offsetX: Int,
                offsetY: Int
        ) : super(width, height) {
            if (geoPoint != null) {
                this.geoPoint = geoPoint
            } else {
                this.geoPoint = GeoPoint(0.0, 0.0)
            }
            this.alignment = alignment
            this.offsetX = offsetX
            this.offsetY = offsetY
        }

        /**
         * Since we cannot use XML files in this project this constructor is useless. Creates a new
         * set of layout parameters. The values are extracted from the supplied attributes set and
         * context.
         *
         * @param c     the application environment
         * @param attrs the set of attributes fom which to extract the layout parameters values
         */
        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {
            geoPoint = GeoPoint(0.0, 0.0)
            alignment = BOTTOM_CENTER
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source) {}

        companion object {
            /**
             * Special value for the alignment requested by a View. TOP_LEFT means that the location
             * will at the top left the View.
             */
            const val TOP_LEFT = 1

            /**
             * Special value for the alignment requested by a View. TOP_RIGHT means that the location
             * will be centered at the top of the View.
             */
            const val TOP_CENTER = 2

            /**
             * Special value for the alignment requested by a View. TOP_RIGHT means that the location
             * will at the top right the View.
             */
            const val TOP_RIGHT = 3

            /**
             * Special value for the alignment requested by a View. CENTER_LEFT means that the location
             * will at the center left the View.
             */
            const val CENTER_LEFT = 4

            /**
             * Special value for the alignment requested by a View. CENTER means that the location will
             * be centered at the center of the View.
             */
            const val CENTER = 5

            /**
             * Special value for the alignment requested by a View. CENTER_RIGHT means that the location
             * will at the center right the View.
             */
            const val CENTER_RIGHT = 6

            /**
             * Special value for the alignment requested by a View. BOTTOM_LEFT means that the location
             * will be at the bottom left of the View.
             */
            const val BOTTOM_LEFT = 7

            /**
             * Special value for the alignment requested by a View. BOTTOM_CENTER means that the
             * location will be centered at the bottom of the view.
             */
            const val BOTTOM_CENTER = 8

            /**
             * Special value for the alignment requested by a View. BOTTOM_RIGHT means that the location
             * will be at the bottom right of the View.
             */
            const val BOTTOM_RIGHT = 9
        }
    }


    /**
     * enables you to programmatically set the tile provider (zip, assets, sqlite, etc)
     *
     * @param base
     * @see MapTileProviderBasic
     */
    fun setTileProvider(base: MapTileProviderBase?) {
        this.mTileProvider!!.detach()
        this.mTileProvider!!.clearTileCache()
        this.mTileProvider = base
        this.mTileProvider!!.tileRequestCompleteHandlers.add(mTileRequestCompleteHandler)
        updateTileSizeForDensity(this.mTileProvider!!.tileSource)
        mMapOverlay = TilesOverlay(this.mTileProvider, this.context, horizontalMapRepetitionEnabled, verticalMapRepetitionEnabled)
        mOverlayManager!!.tilesOverlay = mMapOverlay
        invalidate()
    }

    /**
     * Sets the initial center point of the map. This can be set before the map view is 'ready'
     * meaning that it can be set and honored with the onFirstLayoutListener
     */
    @Deprecated("", ReplaceWith("setExpectedCenter(geoPoint)"))
    fun setInitCenter(geoPoint: IGeoPoint) {
        setExpectedCenter(geoPoint)
    }

    fun getMapScrollX(): Long {
        return mMapScrollX
    }

    fun getMapScrollY(): Long {
        return mMapScrollY
    }

    fun setMapScroll(pMapScrollX: Long, pMapScrollY: Long) {
        mMapScrollX = pMapScrollX
        mMapScrollY = pMapScrollY
        requestLayout() // Allows any views fixed to a Location in the MapView to adjust
    }

    /**
     * Should never be used except by the constructor of Projection.
     * Most of the time you'll want to call [.getMapCenter].
     *
     *
     * This method gives to the Projection the desired map center, typically set by
     * MapView.setExpectedCenter when you want to center a map on a particular point.
     */
    fun getExpectedCenter(): GeoPoint? {
        return mCenter
    }

    /**
     * Deferred setting of the expected next map center computed by the Projection's constructor,
     * with no guarantee it will be 100% respected.
     * [see issue 868](https://github.com/osmdroid/osmdroid/issues/868)
     */
    fun setExpectedCenter(pGeoPoint: IGeoPoint, pOffsetX: Long, pOffsetY: Long) {
        val before: GeoPoint = projection.currentCenter
        mCenter = pGeoPoint as GeoPoint
        setMapScroll(-pOffsetX, -pOffsetY)
        resetProjection()
        val after: GeoPoint = projection.currentCenter
        if (after != before) {
            var event: ScrollEvent? = null
            for (mapListener in mListners) {
                mapListener.onScroll(event ?: ScrollEvent(this, 0, 0).also { event = it })
            }
        }
        invalidate()
    }

    fun setExpectedCenter(pGeoPoint: IGeoPoint) {
        setExpectedCenter(pGeoPoint, 0, 0)
    }

    fun setZoomRounding(pZoomRounding: Boolean) {
        mZoomRounding = pZoomRounding
    }

    companion object {
        @JvmStatic
        var mTileSystem: TileSystem = TileSystemWebMercator()

        @JvmStatic
        fun getTileSystem(): TileSystem {
            return mTileSystem
        }
    }

    fun setTileSystem(pTileSystem: TileSystem) {
        mTileSystem = pTileSystem
    }

    fun getRepository(): MapViewRepository {
        return mRepository
    }

    fun getZoomController(): CustomZoomButtonsController? {
        return mZoomController
    }

    fun getMapOverlay(): TilesOverlay? {
        return mMapOverlay
    }

    fun setDestroyMode(pOnDetach: Boolean) {
        mDestroyModeOnDetach = pOnDetach
    }

    fun getMapCenterOffsetX(): Int {
        return mMapCenterOffsetX
    }

    fun getMapCenterOffsetY(): Int {
        return mMapCenterOffsetY
    }

    fun setMapCenterOffset(pMapCenterOffsetX: Int, pMapCenterOffsetY: Int) {
        mMapCenterOffsetX = pMapCenterOffsetX
        mMapCenterOffsetY = pMapCenterOffsetY
    }
}

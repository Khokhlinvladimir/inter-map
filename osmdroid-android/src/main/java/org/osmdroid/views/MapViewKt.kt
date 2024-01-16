package org.osmdroid.views

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Scroller
import androidx.annotation.RequiresApi
import org.metalev.multitouch.controller.MultiTouchController
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas
import org.osmdroid.api.IGeoPoint
import org.osmdroid.api.IMapController
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration
import org.osmdroid.config.Configuration.instance
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.ITileSource
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

@RequiresApi(Build.VERSION_CODES.CUPCAKE)
class MapViewKt @JvmOverloads constructor(
        context: Context,
        private var tileProvider: MapTileProviderBase? = null,
        private var tileRequestCompleteHandler: Handler? = null,
        attrs: AttributeSet? = null,
        hardwareAccelerated: Boolean = Configuration.instance!!.isMapViewHardwareAccelerated
): ViewGroup(context, attrs), IMapView, MultiTouchObjectCanvas<Any> {

    /**
     * Current zoom level for map tiles.
     */
    private var mZoomLevel = 0.0

    private var mOverlayManager: OverlayManager? = null

    protected var mProjection: Projection? = null

    private var mMapOverlay: TilesOverlay? = null

    private var mGestureDetector: GestureDetector? = null

    /**
     * Handles map scrolling
     */
    private var mScroller: Scroller? = null
    protected var mIsFlinging = false

    /**
     * Set to true when the `Projection` actually adjusted the scroll values
     * Consequence: on this side effect, we must stop the flinging
     */
    private var mImpossibleFlinging = false

    protected val mIsAnimating = AtomicBoolean(false)

    protected var mMinimumZoomLevel: Double? = null
    protected var mMaximumZoomLevel: Double? = null

    private var mController: MapController? = null

    private var mZoomController: CustomZoomButtonsController? = null


    private val mMultiTouchController: MultiTouchController<Any>? = null

    /**
     * Initial pinch gesture pixel (typically, the middle of both fingers)
     */
    private val mMultiTouchScaleInitPoint = PointF()

    /**
     * Initial pinch gesture geo point, computed from [MapViewKt.mMultiTouchScaleInitPoint]
     * and the current Projection
     */
    private val mMultiTouchScaleGeoPoint = GeoPoint(0.0, 0.0)

    /**
     * Current pinch gesture pixel (again, the middle of both fingers)
     * We must ensure that this pixel is the projection of [MapViewKt.mMultiTouchScaleGeoPoint]
     */
    private val mMultiTouchScaleCurrentPoint: PointF? = null


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

    private var mTileProvider: MapTileProviderBase? = null
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

    private val horizontalMapRepetitionEnabled = true
    private val verticalMapRepetitionEnabled = true

    private var mCenter: GeoPoint? = null
    private var mMapScrollX: Long = 0
    private var mMapScrollY: Long = 0
    protected var mListners: List<MapListener> = ArrayList()

    private val mStartAnimationZoom = 0.0

    private val mZoomRounding = false

    private val mRepository = MapViewRepository(this)


    private val mTileSystem: TileSystem = TileSystemWebMercator()


    private val mRescaleScreenRect = Rect() // optimization


    private val mDestroyModeOnDetach = true

    /**
     * The map center used to be projected into the screen center.
     * Now we have a possible offset from the screen center; default offset is [0, 0].
     */
    private val mMapCenterOffsetX = 0
    private val mMapCenterOffsetY = 0

    private val enableFling = true
    private var pauseFling = false // issue 269, boolean used for disabling fling during zoom changes


    // ===========================================================
    // Constructors
    // ===========================================================
init {
    Configuration.instance!!.getOsmdroidTileCache(context)
    if (!isInEditMode) {
        if (!hardwareAccelerated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        mController = MapController(this)
        mScroller = Scroller(context)

        if (tileProvider == null) {
            val tileSource = getTileSourceFromAttributes(attrs)
            tileProvider = MapTileProviderBasic(context.applicationContext, tileSource)
        }

        tileRequestCompleteHandler = tileRequestCompleteHandler ?: SimpleInvalidationHandler(this)
        mTileProvider = tileProvider
        mTileProvider!!.tileRequestCompleteHandlers.add(tileRequestCompleteHandler)
        updateTileSizeForDensity(mTileProvider.getTileSource())

        mMapOverlay = TilesOverlay(mTileProvider, context, horizontalMapRepetitionEnabled, verticalMapRepetitionEnabled)
        mOverlayManager = DefaultOverlayManager(mMapOverlay)

        mZoomController = CustomZoomButtonsController(this)
        mZoomController!!.setOnZoomListener(MapViewZoomListener())
        checkZoomButtons()

        mGestureDetector = GestureDetector(context, MapViewGestureDetectorListener())
        mGestureDetector!!.setOnDoubleTapListener(MapViewDoubleClickListener())

        if (Configuration.instance!!.isMapViewRecyclerFriendly && Build.VERSION.SDK_INT >= 16) {
            this.setHasTransientState(true)
        }

        mZoomController!!.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
    } else {
        // Setup for edit mode in IDE
        mTileRequestCompleteHandler = null
        mController = null
        mZoomController = null
        mScroller = null
        mGestureDetector = null
    }
}// ===========================================================
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
        return mTileProvider
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
    protected fun setProjection(p: Projection) {
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

    @Deprecated("use {@link #setMapCenter(IGeoPoint)}")
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
        if (instance!!.isDebugMapView) Log.d(IMapView.LOGTAG, "Scaling tiles to $size")
        TileSystem.setTileSize(size)
    }

    fun setTileSource(aTileSource: ITileSource) {
        mTileProvider!!.tileSource = aTileSource
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
        val newZoomLevel = getMinZoomLevel().coerceAtLeast(getMaxZoomLevel().coerceAtMost(aZoomLevel))
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
            mTileProvider!!.rescaleCache(pj, newZoomLevel, curZoomLevel, getScreenRect(mRescaleScreenRect))
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
        nextZoom = getMaxZoomLevel().coerceAtMost(nextZoom.coerceAtLeast(getMinZoomLevel()))
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
        zoomToBoundingBox(pBoundingBox!!, pAnimated, pBorderSizeInPixels, getMaxZoomLevel(), null)
    }

    /**
     * Get the current ZoomLevel for the map tiles.
     *
     * @return the current ZoomLevel between 0 (equator) and 18/19(closest), depending on the tile
     * source chosen.
     */

    @Deprecated("", ReplaceWith("getZoomLevelDouble().toInt()"))
    fun getZoomLevel(): Int {
        return getZoomLevelDouble().toInt()
    }

    fun getZoomLevelDouble(): Double {
        return mZoomLevel
    }

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
        return getZoomLevelDouble()
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
    fun getMaxZoomLevel(): Double {
        return if (mMaximumZoomLevel == null) mMapOverlay!!.maximumZoomLevel.toDouble() else mMaximumZoomLevel!!
    }

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
        return mZoomLevel < getMaxZoomLevel()
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


    fun getMapCenter(): IGeoPoint? {
        return getMapCenter(null)
    }

    fun getMapCenter(pReuse: GeoPoint?): IGeoPoint? {
        return projection.fromPixels(width / 2, height / 2, pReuse, false)
    }

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


    override fun generateDefaultLayoutParams(): LayoutParams {
        return MapView.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                null,
                MapView.LayoutParams.BOTTOM_CENTER,
                0,
                0
        )
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MapView.LayoutParams(context, attrs)
    }

    // Override to allow type-checking of LayoutParams.
    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is MapView.LayoutParams
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams? {
        return MapView.LayoutParams(p)
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
    fun myOnLayout(
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

                val lp = child.layoutParams as MapView.LayoutParams
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
                    MapView.LayoutParams.TOP_LEFT -> {
                        childLeft = paddingLeft + x
                        childTop = paddingTop + y
                    }

                    MapView.LayoutParams.TOP_CENTER -> {
                        childLeft = paddingLeft + x - childWidth / 2
                        childTop = paddingTop + y
                    }

                    MapView.LayoutParams.TOP_RIGHT -> {
                        childLeft = paddingLeft + x - childWidth
                        childTop = paddingTop + y
                    }

                    MapView.LayoutParams.CENTER_LEFT -> {
                        childLeft = paddingLeft + x
                        childTop = paddingTop + y - childHeight / 2
                    }

                    MapView.LayoutParams.CENTER -> {
                        childLeft = paddingLeft + x - childWidth / 2
                        childTop = paddingTop + y - childHeight / 2
                    }

                    MapView.LayoutParams.CENTER_RIGHT -> {
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

















    fun setMapScroll(pMapScrollX: Long, pMapScrollY: Long) {
        mMapScrollX = pMapScrollX
        mMapScrollY = pMapScrollY
        requestLayout() // Allows any views fixed to a Location in the MapView to adjust
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

    // ===========================================================
    // Methods
    // ===========================================================



    private fun checkZoomButtons() {
        mZoomController!!.setZoomInEnabled(canZoomIn())
        mZoomController!!.setZoomOutEnabled(canZoomOut())
    }

    fun isLayoutOccurred(): Boolean {
        return mLayoutOccurred
    }



    /**
     * Zoom the map to enclose the specified bounding box, as closely as possible. Must be called
     * after display layout is complete, or screen dimensions are not known, and will always zoom to
     * center of zoom level 0.<br>
     * Suggestion: Check getIntrinsicScreenRect(null).getHeight() &gt; 0
     */
}
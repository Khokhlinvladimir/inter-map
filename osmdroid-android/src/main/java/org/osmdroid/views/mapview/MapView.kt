package org.osmdroid.views.mapview

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Scroller
import androidx.annotation.RequiresApi
import org.metalev.multitouch.controller.MultiTouchController
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas
import org.osmdroid.api.IMapController
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.util.SimpleInvalidationHandler
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.util.TileSystemWebMercator
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapController
import org.osmdroid.views.MapViewRepository
import org.osmdroid.views.OnFirstLayoutListener
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.DefaultOverlayManager
import org.osmdroid.views.overlay.OverlayManager
import org.osmdroid.views.overlay.TilesOverlay
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.CUPCAKE)
class MapView @JvmOverloads constructor(
        context: Context,
        private var tileProvider: MapTileProviderBase? = null,
        private var tileRequestCompleteHandler: Handler? = null,
        attrs: AttributeSet? = null,
        hardwareAccelerated: Boolean = Configuration.instance!!.isMapViewHardwareAccelerated
): ViewGroup(context, attrs), IMapView, MultiTouchObjectCanvas<Any> {

    /**
     * Current zoom level for map tiles.
     */
    private val mZoomLevel = 0.0

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
    private val mImpossibleFlinging = false

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
     * Initial pinch gesture geo point, computed from [MapView.mMultiTouchScaleInitPoint]
     * and the current Projection
     */
    private val mMultiTouchScaleGeoPoint = GeoPoint(0.0, 0.0)

    /**
     * Current pinch gesture pixel (again, the middle of both fingers)
     * We must ensure that this pixel is the projection of [MapView.mMultiTouchScaleGeoPoint]
     */
    private val mMultiTouchScaleCurrentPoint: PointF? = null


    // For rotation
    private val mapOrientation = 0f
    private val mInvalidateRect = Rect()

    private val mScrollableAreaLimitLatitude = false
    private val mScrollableAreaLimitNorth = 0.0
    private val mScrollableAreaLimitSouth = 0.0
    private val mScrollableAreaLimitLongitude = false
    private val mScrollableAreaLimitWest = 0.0
    private val mScrollableAreaLimitEast = 0.0
    private val mScrollableAreaLimitExtraPixelWidth = 0
    private val mScrollableAreaLimitExtraPixelHeight = 0

    private var mTileProvider: MapTileProviderBase? = null
    private var mTileRequestCompleteHandler: Handler? = null
    private val mTilesScaledToDpi = false
    private val mTilesScaleFactor = 1f

    val mRotateScalePoint = Point()

    /* a point that will be reused to lay out added views */
    private val mLayoutPoint = Point()

    // Keep a set of listeners for when the maps have a layout
    private val mOnFirstLayoutListeners = LinkedList<OnFirstLayoutListener>()

    /* becomes true once onLayout has been called for the first time i.e. map is ready to go. */
    private val mLayoutOccurred = false

    private val horizontalMapRepetitionEnabled = true
    private val verticalMapRepetitionEnabled = true

    private val mCenter: GeoPoint? = null
    private val mMapScrollX: Long = 0
    private val mMapScrollY: Long = 0
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






    \
}
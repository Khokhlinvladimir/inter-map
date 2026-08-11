package org.osmdroid.views.overlay.mylocation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import org.osmdroid.api.IMapController
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.library.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.IOverlayMenuProvider
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Overlay.Snappable
import java.util.LinkedList

/**
 * @author Marc Kurtz
 * @author Manuel Stahl
 */
open class MyLocationNewOverlay(myLocationProvider: IMyLocationProvider, mapView: MapView) : Overlay(), IMyLocationConsumer, IOverlayMenuProvider,
    Snappable {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    protected var mPaint: Paint = Paint()
    protected var mCirclePaint: Paint? = Paint()

    protected var mPersonBitmap: Bitmap? = null
    protected var mDirectionArrowBitmap: Bitmap? = null

    protected var mMapView: MapView?

    private var mMapController: IMapController?
    var mMyLocationProvider: IMyLocationProvider? = null

    private val mRunOnFirstFix = LinkedList<Runnable?>()
    private val mDrawPixel = Point()
    private val mSnapPixel = Point()
    private var mHandler: Handler?
    private var mHandlerToken: Any? = Any()

    /**
     * if true, when the user pans the map, follow my location will automatically disable
     * if false, when the user pans the map, the map will continue to follow current location
     */
    var enableAutoStop: Boolean = true
    var lastFix: Location? = null
        private set
    private val mGeoPoint = GeoPoint(0, 0) // for reuse

    /**
     * If enabled, the map is receiving location updates and drawing your location on the map.
     *
     * @return true if enabled, false otherwise
     */
    var isMyLocationEnabled: Boolean = false
        private set

    /**
     * If enabled, the map will center on your current location and automatically scroll as you
     * move. Scrolling the map in the UI will disable.
     *
     * @return true if enabled, false otherwise
     */
    var isFollowLocationEnabled: Boolean = false // follow location updates
        protected set
    /**
     * If enabled, an accuracy circle will be drawn around your current position.
     *
     * @return true if enabled, false otherwise
     */
    /**
     * If enabled, an accuracy circle will be drawn around your current position.
     *
     * @param drawAccuracyEnabled
     * whether the accuracy circle will be enabled
     */
    var isDrawAccuracyEnabled: Boolean = true

    /**
     * Coordinates the feet of the person are located scaled for display density.
     */
    protected val mPersonHotspot: PointF

    protected var mDirectionArrowCenterX: Float = 0f
    protected var mDirectionArrowCenterY: Float = 0f

    // ===========================================================
    // Menu handling methods
    // ===========================================================
    override var isOptionsMenuEnabled: Boolean = true

    private var wasEnabledOnPause = false

    // ===========================================================
    // Constructors
    // ===========================================================
    constructor(mapView: MapView) : this(GpsMyLocationProvider(mapView.getContext()), mapView)

    init {
        mMapView = mapView
        mMapController = mapView.controller
        mCirclePaint!!.setARGB(0, 100, 100, 255)
        mCirclePaint!!.setAntiAlias(true)
        mPaint.setFilterBitmap(true)


        setPersonIcon((mapView.getContext().getResources().getDrawable(R.drawable.person) as BitmapDrawable).getBitmap())
        setDirectionIcon((mapView.getContext().getResources().getDrawable(R.drawable.round_navigation_white_48) as BitmapDrawable).getBitmap())

        // Calculate position of person icon's feet, scaled to screen density
        mPersonHotspot = PointF()
        setPersonAnchor(.5f, .8125f) // anchor for the default icon
        setDirectionAnchor(.5f, .5f) // anchor for the default icon

        mHandler = Handler(Looper.getMainLooper())
        mMyLocationProvider = myLocationProvider
    }

    /**
     * fix for https://github.com/osmdroid/osmdroid/issues/249
     */
    @Deprecated(
        """Use {@link #setPersonIcon(Bitmap)}, {@link #setDirectionIcon(Bitmap)},
	  {@link #setPersonAnchor(float, float)} and {@link #setDirectionAnchor(float, float)} instead"""
    )
    fun setDirectionArrow(personBitmap: Bitmap, directionArrowBitmap: Bitmap) {
        setPersonIcon(personBitmap)
        setDirectionIcon(directionArrowBitmap)
        setDirectionAnchor(.5f, .5f)
    }

    /**
     * @since 6.2.0
     */
    fun setDirectionIcon(pDirectionArrowBitmap: Bitmap) {
        mDirectionArrowBitmap = pDirectionArrowBitmap
    }

    override fun onResume() {
        super.onResume()
        if (wasEnabledOnPause) this.enableFollowLocation()
        this.enableMyLocation()
    }

    override fun onPause() {
        wasEnabledOnPause = this.isFollowLocationEnabled
        this.disableMyLocation()
        super.onPause()
    }

    override fun onDetach(mapView: MapView?) {
        this.disableMyLocation()
        /*if (mPersonBitmap != null) {
			mPersonBitmap.recycle();
		}
		if (mDirectionArrowBitmap != null) {
			mDirectionArrowBitmap.recycle();
		}*/
        this.mMapView = null
        this.mMapController = null
        mHandler = null
        mCirclePaint = null
        //mPersonBitmap = null;
        //mDirectionArrowBitmap = null;
        mHandlerToken = null
        this.lastFix = null
        mMapController = null
        if (mMyLocationProvider != null) mMyLocationProvider!!.destroy()

        mMyLocationProvider = null
        super.onDetach(mapView)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    var myLocationProvider: IMyLocationProvider?
        get() = mMyLocationProvider
        protected set(myLocationProvider) {
            if (myLocationProvider == null) throw RuntimeException(
                "You must pass an IMyLocationProvider to setMyLocationProvider()"
            )

            if (this.isMyLocationEnabled) stopLocationProvider()

            mMyLocationProvider = myLocationProvider
        }

    @Deprecated("Use {@link #setPersonAnchor(float, float)} instead")
    fun setPersonHotspot(x: Float, y: Float) {
        mPersonHotspot.set(x, y)
    }

    protected fun drawMyLocation(canvas: Canvas, pj: Projection, lastFix: Location) {
        pj.toPixels(mGeoPoint, mDrawPixel)

        if (this.isDrawAccuracyEnabled) {
            val radius = (lastFix.getAccuracy()
                    / TileSystem.Companion.GroundResolution(
                lastFix.getLatitude(),
                pj.zoomLevel
            ).toFloat())

            mCirclePaint!!.setAlpha(50)
            mCirclePaint!!.setStyle(Paint.Style.FILL)
            canvas.drawCircle(mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat(), radius, mCirclePaint!!)

            mCirclePaint!!.setAlpha(150)
            mCirclePaint!!.setStyle(Paint.Style.STROKE)
            canvas.drawCircle(mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat(), radius, mCirclePaint!!)
        }

        if (lastFix.hasBearing()) {
            canvas.save()
            // Rotate the icon if we have a GPS fix, take into account if the map is already rotated
            var mapRotation: Float
            mapRotation = lastFix.getBearing()
            if (mapRotation >= 360.0f) mapRotation = mapRotation - 360f
            canvas.rotate(mapRotation, mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat())
            // Draw the bitmap
            canvas.drawBitmap(
                mDirectionArrowBitmap!!, mDrawPixel.x
                        - mDirectionArrowCenterX, mDrawPixel.y - mDirectionArrowCenterY,
                mPaint
            )
            canvas.restore()
        } else {
            canvas.save()
            // Unrotate the icon if the maps are rotated so the little man stays upright
            canvas.rotate(
                -mMapView!!.getMapOrientation(), mDrawPixel.x.toFloat(),
                mDrawPixel.y.toFloat()
            )
            // Draw the bitmap
            canvas.drawBitmap(
                mPersonBitmap!!, mDrawPixel.x - mPersonHotspot.x,
                mDrawPixel.y - mPersonHotspot.y, mPaint
            )
            canvas.restore()
        }
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun draw(c: Canvas, pProjection: Projection) {
        if (this.lastFix != null && this.isMyLocationEnabled) {
            drawMyLocation(c, pProjection, this.lastFix!!)
        }
    }

    override fun onSnapToItem(
        x: Int, y: Int, snapPoint: Point,
        mapView: IMapView?
    ): Boolean {
        if (this.lastFix != null) {
            val pj = mMapView!!.projection
            pj.toPixels(mGeoPoint, mSnapPixel)
            snapPoint.x = mSnapPixel.x
            snapPoint.y = mSnapPixel.y
            val xDiff = (x - mSnapPixel.x).toDouble()
            val yDiff = (y - mSnapPixel.y).toDouble()
            val snap = xDiff * xDiff + yDiff * yDiff < 64
            if (instance!!.isDebugMode) {
                Log.d(IMapView.LOGTAG, "snap=" + snap)
            }
            return snap
        } else {
            return false
        }
    }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView?): Boolean {
        val isSingleFingerDrag = (event.getAction() == MotionEvent.ACTION_MOVE)
                && (event.getPointerCount() == 1)

        if (event.getAction() == MotionEvent.ACTION_DOWN && enableAutoStop) {
            this.disableFollowLocation()
        } else if (isSingleFingerDrag && this.isFollowLocationEnabled) {
            return true // prevent the pan
        }

        return super.onTouchEvent(event, mapView)
    }

    override fun onCreateOptionsMenu(
        pMenu: Menu?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        if (pMenu == null || pMapView == null) return false
        pMenu.add(
            0, MENU_MY_LOCATION + pMenuIdOffset, Menu.NONE,
            pMapView.getContext().getResources().getString(R.string.my_location)
        )
            .setIcon(
                pMapView.getContext().getResources().getDrawable(R.drawable.ic_menu_mylocation)
            )
            .setCheckable(true)

        return true
    }

    override fun onPrepareOptionsMenu(
        pMenu: Menu?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        if (pMenu == null) return false
        pMenu.findItem(MENU_MY_LOCATION + pMenuIdOffset).setChecked(this.isMyLocationEnabled)
        return false
    }

    override fun onOptionsItemSelected(
        pItem: MenuItem?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        if (pItem == null) return false
        val menuId = pItem.getItemId() - pMenuIdOffset
        if (menuId == MENU_MY_LOCATION) {
            if (this.isMyLocationEnabled) {
                this.disableFollowLocation()
                this.disableMyLocation()
            } else {
                this.enableFollowLocation()
                this.enableMyLocation()
            }
            return true
        } else {
            return false
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================
    val myLocation: GeoPoint?
        /**
         * Return a GeoPoint of the last known location, or null if not known.
         */
        get() {
            val location = lastFix ?: return null
            return GeoPoint(location)
        }

    /**
     * Enables "follow" functionality. The map will center on your current location and
     * automatically scroll as you move. Scrolling the map in the UI will disable.
     */
    fun enableFollowLocation() {
        this.isFollowLocationEnabled = true

        // set initial location when enabled
        if (this.isMyLocationEnabled) {
            val location = mMyLocationProvider!!.lastKnownLocation
            if (location != null) {
                setLocation(location)
            }
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView!!.postInvalidate()
        }
    }

    /**
     * Disables "follow" functionality.
     */
    fun disableFollowLocation() {
        if (mMapController != null) mMapController!!.stopAnimation(false)
        this.isFollowLocationEnabled = false
    }

    override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
        if (location != null && mHandler != null) {
            // These location updates can come in from different threads
            mHandler!!.postAtTime(object : Runnable {
                override fun run() {
                    setLocation(location)

                    for (runnable in mRunOnFirstFix) {
                        val t = Thread(runnable)
                        t.setName(this.javaClass.getName() + "#onLocationChanged")
                        t.start()
                    }
                    mRunOnFirstFix.clear()
                }
            }, mHandlerToken, 0)
        }
    }

    protected fun setLocation(location: Location?) {
        this.lastFix = location
        mGeoPoint.setCoords(lastFix!!.getLatitude(), lastFix!!.getLongitude())
        if (this.isFollowLocationEnabled) {
            mMapController!!.animateTo(mGeoPoint)
        } else if (mMapView != null) {
            mMapView!!.postInvalidate()
        }
    }

    /**
     * Enable receiving location updates from the provided IMyLocationProvider and show your
     * location on the maps. You will likely want to call enableMyLocation() from your Activity's
     * Activity.onResume() method, to enable the features of this overlay. Remember to call the
     * corresponding disableMyLocation() in your Activity's Activity.onPause() method to turn off
     * updates when in the background.
     */
    @JvmOverloads
    fun enableMyLocation(myLocationProvider: IMyLocationProvider? = mMyLocationProvider): Boolean {
        if (myLocationProvider == null) return false
        // Set the location provider. This will call stopLocationProvider().
        this.myLocationProvider = myLocationProvider

        val success = mMyLocationProvider!!.startLocationProvider(this)
        this.isMyLocationEnabled = success

        // set initial location when enabled
        if (success) {
            val location = mMyLocationProvider!!.lastKnownLocation
            if (location != null) {
                setLocation(location)
            }
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView!!.postInvalidate()
        }

        return success
    }

    /**
     * Disable location updates
     */
    fun disableMyLocation() {
        this.isMyLocationEnabled = false

        stopLocationProvider()

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView!!.postInvalidate()
        }
    }

    protected fun stopLocationProvider() {
        if (mMyLocationProvider != null) {
            mMyLocationProvider!!.stopLocationProvider()
        }
        if (mHandler != null && mHandlerToken != null) mHandler!!.removeCallbacksAndMessages(mHandlerToken)
    }

    /**
     * Queues a runnable to be executed as soon as we have a location fix. If we already have a fix,
     * we'll execute the runnable immediately and return true. If not, we'll hang on to the runnable
     * and return false; as soon as we get a location fix, we'll run it in in a new thread.
     */
    fun runOnFirstFix(runnable: Runnable?): Boolean {
        if (mMyLocationProvider != null && this.lastFix != null) {
            val t = Thread(runnable)
            t.setName(this.javaClass.getName() + "#runOnFirstFix")
            t.start()
            return true
        } else {
            mRunOnFirstFix.addLast(runnable)
            return false
        }
    }

    /**
     * enables you to change the my location 'person' icon at runtime. note that the
     * hotspot is not updated with this method. see [.setPersonAnchor]
     */
    fun setPersonIcon(icon: Bitmap) {
        mPersonBitmap = icon
    }

    /**
     * Anchors for the person icon
     * Expected values between 0 and 1, 0 being top/left, .5 center and 1 bottom/right
     * @since 6.2.0
     */
    fun setPersonAnchor(pHorizontal: Float, pVertical: Float) {
        mPersonHotspot.set(mPersonBitmap!!.getWidth() * pHorizontal, mPersonBitmap!!.getHeight() * pVertical)
    }

    /**
     * Anchors for the direction icon
     * Expected values between 0 and 1, 0 being top/left, .5 center and 1 bottom/right
     * @since 6.2.0
     */
    fun setDirectionAnchor(pHorizontal: Float, pVertical: Float) {
        mDirectionArrowCenterX = mDirectionArrowBitmap!!.getWidth() * pHorizontal
        mDirectionArrowCenterY = mDirectionArrowBitmap!!.getHeight() * pVertical
    }

    companion object {
        @JvmField
        val MENU_MY_LOCATION: Int = Overlay.getSafeMenuId()
    }
}

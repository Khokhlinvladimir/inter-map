// Created by plusminus on 22:01:11 - 29.09.2008
package org.osmdroid.views.overlay.compass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.WindowManager
import org.osmdroid.library.R
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.IOverlayMenuProvider
import org.osmdroid.views.overlay.Overlay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * Note: the compass overlay causes issues on API 8 devices. See https://github.com/osmdroid/osmdroid/issues/218
 *
 *
 * <br></br><br></br>
 * Note: this class can cause issues if you're also relying on [MapView.addOnFirstLayoutListener]
 * If you happen to be using both, see [Issue 324](https://github.com/osmdroid/osmdroid/issues/324)
 *
 * @author Marc Kurtz
 * @author Manuel Stahl
 */
class CompassOverlay(
    context: Context, orientationProvider: IOrientationProvider,
    protected var mMapView: MapView?
) : Overlay(), IOverlayMenuProvider, IOrientationConsumer {
    private var sSmoothPaint: Paint? = Paint(Paint.FILTER_BITMAP_FLAG)
    private val mDisplay: Display

    var mOrientationProvider: IOrientationProvider? = null

    protected var mCompassFrameBitmap: Bitmap? = null
    protected var mCompassRoseBitmap: Bitmap? = null
    private val mCompassMatrix = Matrix()

    /**
     * If enabled, the map is receiving orientation updates and drawing your location on the map.
     *
     * @return true if enabled, false otherwise
     */
    var isCompassEnabled: Boolean = false
        private set
    private var wasEnabledOnPause = false

    /**
     * +1 for conventional compass, -1 for direction indicator
     */
    private var mMode = 1

    /**
     * The bearing, in degrees east of north, or NaN if none has been set.
     */
    var orientation: Float = Float.NaN
        private set

    /**
     * An offset added to the bearing when drawing the compass.
     * eg. to account for local magnetic declination to indicate true north
     */
    var azimuthOffset: Float = 0.0f

    /**
     * Put the compass in the center of the map regardless of the supplied coordinates.
     */
    /**
     * Ignore mCompassCenter* and put the compass in the center of the map
     */
    var isCompassInCenter: Boolean = false
    private var mCompassCenterX = 35.0f
    private var mCompassCenterY = 35.0f
    private val mCompassRadius = 20.0f

    protected val mCompassFrameCenterX: Float
    protected val mCompassFrameCenterY: Float
    protected val mCompassRoseCenterX: Float
    protected val mCompassRoseCenterY: Float
    protected var mLastRender: Long = 0

    // ===========================================================
    // Menu handling methods
    // ===========================================================
    override var isOptionsMenuEnabled: Boolean = true

    protected val mScale: Float

    /**
     * @since 6.20
     * rendering lag, in milliseconds
     * if the previous rendering was less than this value ago, we don't render again
     */
    private var mLastRenderLag = 500

    /**
     * @since 6.20
     * azimuth/bearing precision, in degrees
     * if the previous bearing was equal to the new one, with this precision, we don't render again
     */
    private var mAzimuthPrecision = 0f

    // ===========================================================
    // Constructors
    // ===========================================================
    constructor(context: Context, mapView: MapView?) : this(context, InternalCompassOrientationProvider(context), mapView)


    init {
        mScale = context.getResources().getDisplayMetrics().density

        val windowManager = context
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplay = windowManager.getDefaultDisplay()

        createCompassFramePicture()
        if (mMode > 0) createCompassRosePicture()
        else createPointerPicture()

        mCompassFrameCenterX = mCompassFrameBitmap!!.width / 2f - 0.5f
        mCompassFrameCenterY = mCompassFrameBitmap!!.height / 2f - 0.5f
        mCompassRoseCenterX = mCompassRoseBitmap!!.width / 2f - 0.5f
        mCompassRoseCenterY = mCompassRoseBitmap!!.height / 2f - 0.5f

        setOrientationProvider(orientationProvider)
    }

    override fun onPause() {
        wasEnabledOnPause = this.isCompassEnabled
        if (mOrientationProvider != null) {
            mOrientationProvider!!.stopOrientationProvider()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (wasEnabledOnPause) {
            this.enableCompass()
        }
    }

    override fun onDetach(mapView: MapView?) {
        this.mMapView = null
        sSmoothPaint = null
        this.disableCompass()
        mOrientationProvider = null
        mCompassFrameBitmap!!.recycle()
        mCompassRoseBitmap!!.recycle()
        super.onDetach(mapView)
    }

    /**
     * @since 6.20
     * rendering lag, in milliseconds
     */
    fun setLastRenderLag(pLastRenderLag: Int) {
        mLastRenderLag = pLastRenderLag
    }

    /**
     * @since 6.20
     * azimuth/bearing precision, in degrees
     */
    fun setAzimuthPrecision(pAzimuthPrecision: Float) {
        mAzimuthPrecision = pAzimuthPrecision
    }

    private fun invalidateCompass() {
        if (mLastRender + mLastRenderLag > System.currentTimeMillis()) return
        mLastRender = System.currentTimeMillis()
        val screenRect = mMapView!!.projection.screenRect
        val frameLeft: Int
        val frameRight: Int
        val frameTop: Int
        val frameBottom: Int
        if (this.isCompassInCenter) {
            frameLeft = (screenRect.left
                    + ceil((screenRect.exactCenterX() - mCompassFrameCenterX).toDouble()).toInt())
            frameTop = (screenRect.top
                    + ceil((screenRect.exactCenterY() - mCompassFrameCenterY).toDouble()).toInt())
            frameRight = (screenRect.left
                    + ceil((screenRect.exactCenterX() + mCompassFrameCenterX).toDouble()).toInt())
            frameBottom = (screenRect.top
                    + ceil((screenRect.exactCenterY() + mCompassFrameCenterY).toDouble()).toInt())
        } else {
            frameLeft = (screenRect.left
                    + ceil((mCompassCenterX * mScale - mCompassFrameCenterX).toDouble()).toInt())
            frameTop = (screenRect.top
                    + ceil((mCompassCenterY * mScale - mCompassFrameCenterY).toDouble()).toInt())
            frameRight = (screenRect.left
                    + ceil((mCompassCenterX * mScale + mCompassFrameCenterX).toDouble()).toInt())
            frameBottom = (screenRect.top
                    + ceil((mCompassCenterY * mScale + mCompassFrameCenterY).toDouble()).toInt())
        }

        // Expand by 2 to cover stroke width
        mMapView!!.postInvalidateMapCoordinates(
            frameLeft - 2, frameTop - 2, frameRight + 2,
            frameBottom + 2
        )
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    fun setCompassCenter(x: Float, y: Float) {
        mCompassCenterX = x
        mCompassCenterY = y
    }

    fun getOrientationProvider(): IOrientationProvider? = mOrientationProvider

    @Throws(RuntimeException::class)
    fun setOrientationProvider(orientationProvider: IOrientationProvider?) {
        if (orientationProvider == null) throw RuntimeException(
            "You must pass an IOrientationProvider to setOrientationProvider()"
        )
        if (this.isCompassEnabled) mOrientationProvider!!.stopOrientationProvider()
        mOrientationProvider = orientationProvider
    }

    protected fun drawCompass(canvas: Canvas, bearing: Float, screenRect: Rect?) {
        val proj = mMapView!!.projection

        val centerX: Float
        val centerY: Float
        if (this.isCompassInCenter) {
            val rect = proj.screenRect
            centerX = rect.exactCenterX()
            centerY = rect.exactCenterY()
        } else {
            centerX = mCompassCenterX * mScale
            centerY = mCompassCenterY * mScale
        }

        mCompassMatrix.setTranslate(-mCompassFrameCenterX, -mCompassFrameCenterY)
        mCompassMatrix.postTranslate(centerX, centerY)

        proj.save(canvas, false, true)
        canvas.concat(mCompassMatrix)
        canvas.drawBitmap(mCompassFrameBitmap!!, 0f, 0f, sSmoothPaint)
        proj.restore(canvas, true)

        mCompassMatrix.setRotate(-bearing, mCompassRoseCenterX, mCompassRoseCenterY)
        mCompassMatrix.postTranslate(-mCompassRoseCenterX, -mCompassRoseCenterY)
        mCompassMatrix.postTranslate(centerX, centerY)

        proj.save(canvas, false, true)
        canvas.concat(mCompassMatrix)
        canvas.drawBitmap(mCompassRoseBitmap!!, 0f, 0f, sSmoothPaint)
        proj.restore(canvas, true)
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun draw(c: Canvas, pProjection: Projection) {
        if (this.isCompassEnabled && !this.orientation.isNaN()) {
            drawCompass(
                c, mMode * (this.orientation + this.azimuthOffset + this.displayOrientation), pProjection
                    .screenRect
            )
        }
    }

    override fun onCreateOptionsMenu(
        pMenu: Menu?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        pMenu ?: return false
        pMapView ?: return false
        pMenu.add(
            0, MENU_COMPASS + pMenuIdOffset, Menu.NONE,
            pMapView.getContext().getResources().getString(R.string.compass)
        )

            .setIcon(pMapView.getContext().getResources().getDrawable(R.drawable.ic_menu_compass))
            .setCheckable(true)

        return true
    }

    override fun onPrepareOptionsMenu(
        pMenu: Menu?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        pMenu ?: return false
        pMenu.findItem(MENU_COMPASS + pMenuIdOffset).setChecked(this.isCompassEnabled)
        return false
    }

    override fun onOptionsItemSelected(
        pItem: MenuItem?, pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        pItem ?: return false
        val menuId = pItem.getItemId() - pMenuIdOffset
        if (menuId == MENU_COMPASS) {
            if (this.isCompassEnabled) {
                this.disableCompass()
            } else {
                this.enableCompass()
            }
            return true
        } else {
            return false
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================
    override fun onOrientationChanged(orientation: kotlin.Float, source: IOrientationProvider?) {
        if (this.orientation.isNaN() || abs(this.orientation - orientation) >= mAzimuthPrecision) {
            this.orientation = orientation
            this.invalidateCompass()
        }
    }

    /**
     * Enable receiving orientation updates from the provided IOrientationProvider and show a
     * compass on the map. You will likely want to call enableCompass() from your Activity's
     * Activity.onResume() method, to enable the features of this overlay. Remember to call the
     * corresponding disableCompass() in your Activity's Activity.onPause() method to turn off
     * updates when in the background.
     */
    @JvmOverloads
    fun enableCompass(orientationProvider: IOrientationProvider = mOrientationProvider!!): Boolean {
        // Set the orientation provider. This will call stopOrientationProvider().
        setOrientationProvider(orientationProvider)

        val success = mOrientationProvider!!.startOrientationProvider(this)
        this.isCompassEnabled = success

        // Update the screen to see changes take effect
        if (mMapView != null) {
            this.invalidateCompass()
        }

        return success
    }

    /**
     * Disable orientation updates.
     *
     *
     * Note the behavior has changed since v6.0.0. This method no longer releases
     * references to the orientation provider. Instead, that happens in the onDetached
     * method.
     */
    fun disableCompass() {
        this.isCompassEnabled = false

        if (mOrientationProvider != null) {
            mOrientationProvider!!.stopOrientationProvider()
        }

        // Reset values
        this.orientation = kotlin.Float.Companion.NaN

        // Update the screen to see changes take effect
        if (mMapView != null) {
            this.invalidateCompass()
        }
    }

    var isPointerMode: Boolean
        /**
         * @return true if we are in pointer mode, instead of compass mode
         * @since 6.0.0
         */
        get() = mMode < 0
        /**
         * The compass can operate in two modes.
         *
         *  * false - a conventional compass needle pointing north/south (false, default)
         *  * true - a pointer arrow that indicates the device's real world orientation on the map (true)
         *
         * A different picture is used in each case.
         *
         * @param usePointArrow if true the pointer arrow is used, otherwise a compass rose is used
         * @since 6.0.0
         */
        set(usePointArrow) {
            if (usePointArrow) {
                mMode = -1
                createPointerPicture()
            } else {
                mMode = 1
                createCompassRosePicture()
            }
        }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    private fun calculatePointOnCircle(
        centerX: kotlin.Float, centerY: kotlin.Float,
        radius: kotlin.Float, degrees: kotlin.Float
    ): Point {
        // for trigonometry, 0 is pointing east, so subtract 90
        // compass degrees are the wrong way round
        val dblRadians = Math.toRadians((-degrees + 90).toDouble())

        val intX = (radius * cos(dblRadians)).toInt()
        val intY = (radius * sin(dblRadians)).toInt()

        return Point(centerX.toInt() + intX, centerY.toInt() - intY)
    }

    private fun drawTriangle(
        canvas: Canvas, x: kotlin.Float, y: kotlin.Float,
        radius: kotlin.Float, degrees: kotlin.Float, paint: Paint
    ) {
        canvas.save()
        val point = this.calculatePointOnCircle(x, y, radius, degrees)
        canvas.rotate(degrees, point.x.toFloat(), point.y.toFloat())
        val p = Path()
        p.moveTo(point.x - 2 * mScale, point.y.toFloat())
        p.lineTo(point.x + 2 * mScale, point.y.toFloat())
        p.lineTo(point.x.toFloat(), point.y - 5 * mScale)
        p.close()
        canvas.drawPath(p, paint)
        canvas.restore()
    }

    private val displayOrientation: Int
        get() {
            when (mDisplay.getRotation()) {
                Surface.ROTATION_90 -> return 90
                Surface.ROTATION_180 -> return 180
                Surface.ROTATION_270 -> return 270
                Surface.ROTATION_0 -> return 0
                else -> return 0
            }
        }

    private fun createCompassFramePicture() {
        // The inside of the compass is white and transparent
        val innerPaint = Paint()
        innerPaint.setColor(Color.WHITE)
        innerPaint.setAntiAlias(true)
        innerPaint.setStyle(Paint.Style.FILL)
        innerPaint.setAlpha(200)

        // The outer part (circle and little triangles) is gray and transparent
        val outerPaint = Paint()
        outerPaint.setColor(Color.GRAY)
        outerPaint.setAntiAlias(true)
        outerPaint.setStyle(Paint.Style.STROKE)
        outerPaint.setStrokeWidth(2.0f)
        outerPaint.setAlpha(200)

        val picBorderWidthAndHeight = ((mCompassRadius + 5) * 2 * mScale).toInt()
        val center = picBorderWidthAndHeight / 2
        if (mCompassFrameBitmap != null) mCompassFrameBitmap!!.recycle()
        mCompassFrameBitmap = Bitmap.createBitmap(
            picBorderWidthAndHeight, picBorderWidthAndHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(mCompassFrameBitmap!!)

        // draw compass inner circle and border
        canvas.drawCircle(center.toFloat(), center.toFloat(), mCompassRadius * mScale, innerPaint)
        canvas.drawCircle(center.toFloat(), center.toFloat(), mCompassRadius * mScale, outerPaint)

        // Draw little triangles north, south, west and east (don't move)
        // to make those move use "-bearing + 0" etc. (Note: that would mean to draw the triangles
        // in the onDraw() method)
        drawTriangle(canvas, center.toFloat(), center.toFloat(), mCompassRadius * mScale, 0f, outerPaint)
        drawTriangle(canvas, center.toFloat(), center.toFloat(), mCompassRadius * mScale, 90f, outerPaint)
        drawTriangle(canvas, center.toFloat(), center.toFloat(), mCompassRadius * mScale, 180f, outerPaint)
        drawTriangle(canvas, center.toFloat(), center.toFloat(), mCompassRadius * mScale, 270f, outerPaint)
    }

    /**
     * A conventional red and black compass needle.
     */
    private fun createCompassRosePicture() {
        // Paint design of north triangle (it's common to paint north in red color)
        val northPaint = Paint()
        northPaint.setColor(-0x600000)
        northPaint.setAntiAlias(true)
        northPaint.setStyle(Paint.Style.FILL)
        northPaint.setAlpha(220)

        // Paint design of south triangle (black)
        val southPaint = Paint()
        southPaint.setColor(Color.BLACK)
        southPaint.setAntiAlias(true)
        southPaint.setStyle(Paint.Style.FILL)
        southPaint.setAlpha(220)

        // Create a little white dot in the middle of the compass rose
        val centerPaint = Paint()
        centerPaint.setColor(Color.WHITE)
        centerPaint.setAntiAlias(true)
        centerPaint.setStyle(Paint.Style.FILL)
        centerPaint.setAlpha(220)

        val picBorderWidthAndHeight = ((mCompassRadius + 5) * 2 * mScale).toInt()
        val center = picBorderWidthAndHeight / 2

        if (mCompassRoseBitmap != null) mCompassRoseBitmap!!.recycle()
        mCompassRoseBitmap = Bitmap.createBitmap(
            picBorderWidthAndHeight, picBorderWidthAndHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(mCompassRoseBitmap!!)

        // Triangle pointing north
        val pathNorth = Path()
        pathNorth.moveTo(center.toFloat(), center - (mCompassRadius - 3) * mScale)
        pathNorth.lineTo(center + 4 * mScale, center.toFloat())
        pathNorth.lineTo(center - 4 * mScale, center.toFloat())
        pathNorth.lineTo(center.toFloat(), center - (mCompassRadius - 3) * mScale)
        pathNorth.close()
        canvas.drawPath(pathNorth, northPaint)

        // Triangle pointing south
        val pathSouth = Path()
        pathSouth.moveTo(center.toFloat(), center + (mCompassRadius - 3) * mScale)
        pathSouth.lineTo(center + 4 * mScale, center.toFloat())
        pathSouth.lineTo(center - 4 * mScale, center.toFloat())
        pathSouth.lineTo(center.toFloat(), center + (mCompassRadius - 3) * mScale)
        pathSouth.close()
        canvas.drawPath(pathSouth, southPaint)

        // Draw a little white dot in the middle
        canvas.drawCircle(center.toFloat(), center.toFloat(), 2f, centerPaint)
    }

    /**
     * A black pointer arrow.
     */
    private fun createPointerPicture() {
        val arrowPaint = Paint()
        arrowPaint.setColor(Color.BLACK)
        arrowPaint.setAntiAlias(true)
        arrowPaint.setStyle(Paint.Style.FILL)
        arrowPaint.setAlpha(220)

        // Create a little white dot in the middle of the compass rose
        val centerPaint = Paint()
        centerPaint.setColor(Color.WHITE)
        centerPaint.setAntiAlias(true)
        centerPaint.setStyle(Paint.Style.FILL)
        centerPaint.setAlpha(220)

        val picBorderWidthAndHeight = ((mCompassRadius + 5) * 2 * mScale).toInt()
        val center = picBorderWidthAndHeight / 2

        if (mCompassRoseBitmap != null) mCompassRoseBitmap!!.recycle()
        mCompassRoseBitmap = Bitmap.createBitmap(
            picBorderWidthAndHeight, picBorderWidthAndHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(mCompassRoseBitmap!!)

        // Arrow comprised of 2 triangles
        val pathArrow = Path()
        pathArrow.moveTo(center.toFloat(), center - (mCompassRadius - 3) * mScale)
        pathArrow.lineTo(center + 4 * mScale, center + (mCompassRadius - 3) * mScale)
        pathArrow.lineTo(center.toFloat(), center + 0.5f * (mCompassRadius - 3) * mScale)
        pathArrow.lineTo(center - 4 * mScale, center + (mCompassRadius - 3) * mScale)
        pathArrow.lineTo(center.toFloat(), center - (mCompassRadius - 3) * mScale)
        pathArrow.close()
        canvas.drawPath(pathArrow, arrowPaint)

        // Draw a little white dot in the middle
        canvas.drawCircle(center.toFloat(), center.toFloat(), 2f, centerPaint)
    }

    companion object {
        val MENU_COMPASS: Int = getSafeMenuId()
    }
}

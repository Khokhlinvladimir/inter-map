package org.osmdroid.views.overlay.mylocation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.osmdroid.library.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

class DirectedLocationOverlay(ctx: Context) : Overlay() {
    protected var mPaint: Paint? = Paint()
    protected var mAccuracyPaint: Paint? = Paint()

    protected var DIRECTION_ARROW: Bitmap? = null

    var location: GeoPoint? = null
    protected var mBearing: Float = 0f

    private val directionRotater = Matrix()
    private val screenCoords = Point()

    private var DIRECTION_ARROW_CENTER_X = 0f
    private var DIRECTION_ARROW_CENTER_Y = 0f
    private var DIRECTION_ARROW_WIDTH = 0
    private var DIRECTION_ARROW_HEIGHT = 0

    private var mAccuracy = 0
    private var mShowAccuracy = true

    init {
        setDirectionArrow(requireNotNull(ContextCompat.getDrawable(ctx, R.drawable.ic_osm_navigation)).toBitmap())

        this.mAccuracyPaint!!.setStrokeWidth(2f)
        this.mAccuracyPaint!!.setColor(Color.BLUE)
        this.mAccuracyPaint!!.setAntiAlias(true)
    }

    fun setDirectionArrow(image: Bitmap) {
        this.DIRECTION_ARROW = image
        this.DIRECTION_ARROW_CENTER_X = this.DIRECTION_ARROW!!.getWidth() / 2f - 0.5f
        this.DIRECTION_ARROW_CENTER_Y = this.DIRECTION_ARROW!!.getHeight() / 2f - 0.5f
        this.DIRECTION_ARROW_HEIGHT = this.DIRECTION_ARROW!!.getHeight()
        this.DIRECTION_ARROW_WIDTH = this.DIRECTION_ARROW!!.getWidth()
    }

    fun setShowAccuracy(pShowIt: Boolean) {
        this.mShowAccuracy = pShowIt
    }

    /**
     * @param pAccuracy in Meters
     */
    fun setAccuracy(pAccuracy: Int) {
        this.mAccuracy = pAccuracy
    }

    fun setBearing(aHeading: Float) {
        this.mBearing = aHeading
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun onDetach(view: MapView?) {
        mPaint = null
        mAccuracyPaint = null
    }

    override fun draw(c: Canvas, pj: Projection) {
        if (this.location != null) {
            pj.toPixels(this.location, screenCoords)

            if (this.mShowAccuracy && this.mAccuracy > 10) {
                val accuracyRadius = pj.metersToPixels(this.mAccuracy.toFloat(), location!!.latitude, pj.zoomLevel)
                /* Only draw if the DirectionArrow doesn't cover it. */
                if (accuracyRadius > 8) {
                    /* Draw the inner shadow. */
                    this.mAccuracyPaint!!.setAntiAlias(false)
                    this.mAccuracyPaint!!.setAlpha(30)
                    this.mAccuracyPaint!!.setStyle(Paint.Style.FILL)
                    c.drawCircle(
                        screenCoords.x.toFloat(), screenCoords.y.toFloat(), accuracyRadius,
                        this.mAccuracyPaint!!
                    )

                    /* Draw the edge. */
                    this.mAccuracyPaint!!.setAntiAlias(true)
                    this.mAccuracyPaint!!.setAlpha(150)
                    this.mAccuracyPaint!!.setStyle(Paint.Style.STROKE)
                    c.drawCircle(
                        screenCoords.x.toFloat(), screenCoords.y.toFloat(), accuracyRadius,
                        this.mAccuracyPaint!!
                    )
                }
            }

            /*
             * Rotate the direction-Arrow according to the bearing we are driving. And draw it to
             * the canvas.
             */
            this.directionRotater.setRotate(
                this.mBearing, DIRECTION_ARROW_CENTER_X,
                DIRECTION_ARROW_CENTER_Y
            )
            val rotatedDirection = Bitmap.createBitmap(
                DIRECTION_ARROW!!, 0, 0,
                DIRECTION_ARROW_WIDTH, DIRECTION_ARROW_HEIGHT, this.directionRotater, false
            )
            c.drawBitmap(
                rotatedDirection, (screenCoords.x - rotatedDirection.getWidth() / 2).toFloat(),
                (screenCoords.y - rotatedDirection.getHeight() / 2).toFloat(), this.mPaint
            )
        }
    }
}

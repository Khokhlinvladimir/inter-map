package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import org.osmdroid.library.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants
import org.osmdroid.util.constants.GeoConstants.UnitOfMeasure
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import java.util.Locale
import kotlin.math.pow

/**
 * ScaleBarOverlay.java
 *
 *
 * Puts a scale bar in the top-left corner of the screen, offset by a configurable
 * number of pixels. The bar is scaled to 1-inch length by querying for the physical
 * DPI of the screen. The size of the bar is printed between the tick marks. A
 * vertical (longitude) scale can be enabled. Scale is printed in metric (kilometers,
 * meters), imperial (miles, feet) and nautical (nautical miles, feet).
 *
 *
 * Author: Erik Burrows, Griffin Systems LLC
 * erik@griffinsystems.org
 *
 *
 * Change Log:
 * 2010-10-08: Inclusion to osmdroid trunk
 * 2015-12-17: Allow for top, bottom, left or right placement by W.  Strickling
 *
 *
 * Usage:
 * `
 * MapView map = new MapView(...);
 * ScaleBarOverlay scaleBar = new ScaleBarOverlay(map); // Thiw is an important change of calling!
 *
 *
 * map.getOverlays().add(scaleBar);
` *
 *
 *
 * To Do List:
 * 1. Allow for top, bottom, left or right placement. // done in this changement
 * 2. Scale bar to precise displayed scale text after rounding.
 */
class ScaleBarOverlay private constructor(
    private var mMapView: MapView?, // Internal
    private var context: Context?, pMapWidth: Int, pMapHeight: Int
) : Overlay(), GeoConstants {
    enum class UnitsOfMeasure {
        metric, imperial, nautical
    }

    // Defaults
    var xOffset: Int = 10
    var yOffset: Int = 10
    private var mMinZoom: Double = 0.0

    private var mUnitsOfMeasure: UnitsOfMeasure = UnitsOfMeasure.metric

    var latitudeBar: Boolean = true
    var longitudeBar: Boolean = false

    protected var mAlignBottom: Boolean = false
    protected var mAlignRight: Boolean = false


    protected val barPath: Path = Path()
    protected val latitudeBarRect: Rect = Rect()
    protected val longitudeBarRect: Rect = Rect()

    private var lastZoomLevel = -1.0
    private var lastLatitude = 0.0

    var xdpi: Float
    var ydpi: Float
    var screenWidth: Int
    var screenHeight: Int

    private var barPaint: Paint?
    private var bgPaint: Paint?
    private var textPaint: Paint?

    private var centred = false
    private var adjustLength = false
    private var maxLength: Float

    /**
     * @since 6.1.0
     */
    private val mMapWidth: Int

    /**
     * @since 6.1.0
     */
    private val mMapHeight: Int

    // ===========================================================
    // Constructors
    // ===========================================================
    constructor(mapView: MapView) : this(mapView, mapView.getContext(), 0, 0)

    /**
     * @since 6.1.0
     */
    constructor(pContext: Context?, pMapWidth: Int, pMapHeight: Int) : this(null, pContext, pMapWidth, pMapHeight)

    /**
     * @since 6.1.0
     */
    init {
        context = context
        mMapWidth = pMapWidth
        mMapHeight = pMapHeight

        val dm = context!!.getResources().getDisplayMetrics()

        this.barPaint = Paint()
        this.barPaint!!.setColor(Color.BLACK)
        this.barPaint!!.setAntiAlias(true)
        this.barPaint!!.setStyle(Paint.Style.STROKE)
        this.barPaint!!.setAlpha(255)
        this.barPaint!!.setStrokeWidth(2 * dm.density)
        this.bgPaint = null

        this.textPaint = Paint()
        this.textPaint!!.setColor(Color.BLACK)
        this.textPaint!!.setAntiAlias(true)
        this.textPaint!!.setStyle(Paint.Style.FILL)
        this.textPaint!!.setAlpha(255)
        this.textPaint!!.setTextSize(10 * dm.density)

        this.xdpi = dm.xdpi
        this.ydpi = dm.ydpi

        this.screenWidth = dm.widthPixels
        this.screenHeight = dm.heightPixels

        // DPI corrections for specific models
        var manufacturer: String? = null
        try {
            val field = Build::class.java.getField("MANUFACTURER")
            manufacturer = field.get(null) as String?
        } catch (ignore: Exception) {
        }

        if ("motorola" == manufacturer && "DROIDX" == Build.MODEL) {
            // If the screen is rotated, flip the x and y dpi values

            val windowManager = this.context!!
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            if (windowManager != null && windowManager.getDefaultDisplay().getOrientation() > 0) {
                this.xdpi = (this.screenWidth / 3.75).toFloat()
                this.ydpi = (this.screenHeight / 2.1).toFloat()
            } else {
                this.xdpi = (this.screenWidth / 2.1).toFloat()
                this.ydpi = (this.screenHeight / 3.75).toFloat()
            }
        } else if ("motorola" == manufacturer && "Droid" == Build.MODEL) {
            // http://www.mail-archive.com/android-developers@googlegroups.com/msg109497.html
            this.xdpi = 264f
            this.ydpi = 264f
        }

        // set default max length to 1 inch
        maxLength = 2.54f
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    /**
     * Sets the minimum zoom level for the scale bar to be drawn.
     *
     * @param zoom minimum zoom level
     */
    fun setMinZoom(zoom: Double) {
        mMinZoom = zoom
    }

    /**
     * Sets the scale bar screen offset for the bar. Note: if the bar is set to be drawn centered,
     * this will be the middle of the bar, otherwise the top left corner.
     *
     * @param x x screen offset
     * @param y z screen offset
     */
    fun setScaleBarOffset(x: Int, y: Int) {
        xOffset = x
        yOffset = y
    }

    /**
     * Sets the bar's line width. (the default is 2)
     *
     * @param width the new line width
     */
    fun setLineWidth(width: Float) {
        this.barPaint!!.setStrokeWidth(width)
    }

    /**
     * Sets the text size. (the default is 12)
     *
     * @param size the new text size
     */
    fun setTextSize(size: Float) {
        this.textPaint!!.setTextSize(size)
    }

    /**
     * Sets the units of measure to be shown in the scale bar
     */
    fun setUnitsOfMeasure(unitsOfMeasure: UnitsOfMeasure) {
        mUnitsOfMeasure = unitsOfMeasure
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    /**
     * Gets the units of measure to be shown in the scale bar
     */
    fun getUnitsOfMeasure(): UnitsOfMeasure {
        return mUnitsOfMeasure
    }

    /**
     * Latitudinal / horizontal scale bar flag
     *
     * @param latitude
     */
    fun drawLatitudeScale(latitude: Boolean) {
        this.latitudeBar = latitude
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    /**
     * Longitudinal / vertical scale bar flag
     *
     * @param longitude
     */
    fun drawLongitudeScale(longitude: Boolean) {
        this.longitudeBar = longitude
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    /**
     * Flag to draw the bar centered around the set offset coordinates or to the right/bottom of the
     * coordinates (default)
     *
     * @param centred set true to centre the bar around the given screen coordinates
     */
    fun setCentred(centred: Boolean) {
        this.centred = centred
        mAlignBottom = !centred
        mAlignRight = !centred
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    fun setAlignBottom(alignBottom: Boolean) {
        this.centred = false
        mAlignBottom = alignBottom
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    fun setAlignRight(alignRight: Boolean) {
        this.centred = false
        mAlignRight = alignRight
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    /**
     * Return's the paint used to draw the bar
     *
     * @return the paint used to draw the bar
     */
    fun getBarPaint(): Paint? {
        return barPaint
    }

    /**
     * Sets the paint for drawing the bar
     *
     * @param pBarPaint bar drawing paint
     */
    fun setBarPaint(pBarPaint: Paint) {
        requireNotNull(pBarPaint) { "pBarPaint argument cannot be null" }
        barPaint = pBarPaint
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    /**
     * Returns the paint used to draw the text
     *
     * @return the paint used to draw the text
     */
    fun getTextPaint(): Paint? {
        return textPaint
    }

    /**
     * Sets the paint for drawing the text
     *
     * @param pTextPaint text drawing paint
     */
    fun setTextPaint(pTextPaint: Paint) {
        requireNotNull(pTextPaint) { "pTextPaint argument cannot be null" }
        textPaint = pTextPaint
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    /**
     * Sets the background paint. Set to null to disable drawing of background (default)
     *
     * @param pBgPaint the paint for colouring the bar background
     */
    fun setBackgroundPaint(pBgPaint: Paint?) {
        bgPaint = pBgPaint
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    /**
     * If enabled, the bar will automatically adjust the length to reflect a round number (starting
     * with 1, 2 or 5). If disabled, the bar will always be drawn in full length representing a
     * fractional distance.
     */
    fun setEnableAdjustLength(adjustLength: Boolean) {
        this.adjustLength = adjustLength
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    /**
     * Sets the maximum bar length. If adjustLength is disabled this will match exactly the length
     * of the bar. If adjustLength is enabled, the bar will be shortened to reflect a round number
     * in length.
     *
     * @param pMaxLengthInCm maximum length of the bar in the screen in cm. Default is 2.54 (=1 inch)
     */
    fun setMaxLength(pMaxLengthInCm: Float) {
        this.maxLength = pMaxLengthInCm
        lastZoomLevel = -1.0 // Force redraw of scalebar
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun draw(c: Canvas, projection: Projection) {
        val zoomLevel = projection.zoomLevel

        if (zoomLevel < mMinZoom) {
            return
        }
        val rect = projection.intrinsicScreenRect
        val _screenWidth = rect.width()
        val _screenHeight = rect.height()
        val screenSizeChanged = _screenHeight != screenHeight || _screenWidth != screenWidth
        screenHeight = _screenHeight
        screenWidth = _screenWidth
        val center = projection.fromPixels(screenWidth / 2, screenHeight / 2, null)
        if (zoomLevel != lastZoomLevel || center.latitude != lastLatitude || screenSizeChanged) {
            lastZoomLevel = zoomLevel
            lastLatitude = center.latitude
            rebuildBarPath(projection)
        }

        var offsetX = xOffset
        var offsetY = yOffset
        if (mAlignBottom) offsetY *= -1
        if (mAlignRight) offsetX *= -1
        if (centred && latitudeBar) offsetX += -latitudeBarRect.width() / 2
        if (centred && longitudeBar) offsetY += -longitudeBarRect.height() / 2

        projection.save(c, false, true)
        c.translate(offsetX.toFloat(), offsetY.toFloat())

        if (latitudeBar && bgPaint != null) c.drawRect(latitudeBarRect, bgPaint!!)
        if (longitudeBar && bgPaint != null) {
            // Don't draw on top of latitude background...
            val offsetTop = if (latitudeBar) latitudeBarRect.height() else 0
            c.drawRect(
                longitudeBarRect.left.toFloat(), (longitudeBarRect.top + offsetTop).toFloat(),
                longitudeBarRect.right.toFloat(), longitudeBarRect.bottom.toFloat(), bgPaint!!
            )
        }
        c.drawPath(barPath, barPaint!!)
        if (latitudeBar) {
            drawLatitudeText(c, projection)
        }
        if (longitudeBar) {
            drawLongitudeText(c, projection)
        }
        projection.restore(c, true)
    }

    // ===========================================================
    // Methods
    // ===========================================================
    fun disableScaleBar() {
        setEnabled(false)
    }

    fun enableScaleBar() {
        setEnabled(true)
    }

    private fun drawLatitudeText(canvas: Canvas, projection: Projection) {
        // calculate dots per centimeter
        val xdpcm = (xdpi / 2.54).toInt()

        // get length in pixel
        val xLen = (maxLength * xdpcm).toInt()

        // Two points, xLen apart, at scale bar screen location
        val p1 = projection.fromPixels((screenWidth / 2) - (xLen / 2), yOffset, null)
        val p2 = projection.fromPixels((screenWidth / 2) + (xLen / 2), yOffset, null)

        // get distance in meters between points
        val xMeters = (p1 as GeoPoint).distanceToAsDouble(p2)
        // get adjusted distance, shortened to the next lower number starting with 1, 2 or 5
        val xMetersAdjusted = if (this.adjustLength) adjustScaleBarLength(xMeters) else xMeters
        // get adjusted length in pixels
        val xBarLengthPixels = (xLen * xMetersAdjusted / xMeters).toInt()

        // create text
        val xMsg = scaleBarLengthText(xMetersAdjusted)
        textPaint!!.getTextBounds(xMsg, 0, xMsg.length, sTextBoundsRect)
        val xTextSpacing = (sTextBoundsRect.height() / 5.0).toInt()

        var x = (xBarLengthPixels / 2 - sTextBoundsRect.width() / 2).toFloat()
        if (mAlignRight) x += (screenWidth - xBarLengthPixels).toFloat()
        val y: Float
        if (mAlignBottom) {
            y = (screenHeight - xTextSpacing * 2).toFloat()
        } else y = (sTextBoundsRect.height() + xTextSpacing).toFloat()
        canvas.drawText(xMsg, x, y, textPaint!!)
    }

    private fun drawLongitudeText(canvas: Canvas, projection: Projection) {
        // calculate dots per centimeter
        val ydpcm = (ydpi / 2.54).toInt()

        // get length in pixel
        val yLen = (maxLength * ydpcm).toInt()

        // Two points, yLen apart, at scale bar screen location
        val p1 = projection
            .fromPixels(screenWidth / 2, (screenHeight / 2) - (yLen / 2), null)
        val p2 = projection
            .fromPixels(screenWidth / 2, (screenHeight / 2) + (yLen / 2), null)

        // get distance in meters between points
        val yMeters = (p1 as GeoPoint).distanceToAsDouble(p2)
        // get adjusted distance, shortened to the next lower number starting with 1, 2 or 5
        val yMetersAdjusted = if (this.adjustLength) adjustScaleBarLength(yMeters) else yMeters
        // get adjusted length in pixels
        val yBarLengthPixels = (yLen * yMetersAdjusted / yMeters).toInt()

        // create text
        val yMsg = scaleBarLengthText(yMetersAdjusted)
        textPaint!!.getTextBounds(yMsg, 0, yMsg.length, sTextBoundsRect)
        val yTextSpacing = (sTextBoundsRect.height() / 5.0).toInt()

        val x: Float
        if (mAlignRight) {
            x = (screenWidth - yTextSpacing * 2).toFloat()
        } else x = (sTextBoundsRect.height() + yTextSpacing).toFloat()
        var y = (yBarLengthPixels / 2 + sTextBoundsRect.width() / 2).toFloat()
        if (mAlignBottom) y += (screenHeight - yBarLengthPixels).toFloat()
        canvas.save()
        canvas.rotate(-90f, x, y)
        canvas.drawText(yMsg, x, y, textPaint!!)
        canvas.restore()
    }

    protected fun rebuildBarPath(projection: Projection) {   //** modified to protected
        // We want the scale bar to be as long as the closest round-number miles/kilometers
        // to 1-inch at the latitude at the current center of the screen.

        // calculate dots per centimeter

        val xdpcm = (xdpi / 2.54).toInt()
        val ydpcm = (ydpi / 2.54).toInt()

        // get length in pixel
        val xLen = (maxLength * xdpcm).toInt()
        val yLen = (maxLength * ydpcm).toInt()

        // Two points, xLen apart, at scale bar screen location
        var p1 = projection.fromPixels((screenWidth / 2) - (xLen / 2), yOffset, null)
        var p2 = projection.fromPixels((screenWidth / 2) + (xLen / 2), yOffset, null)

        // get distance in meters between points
        val xMeters = (p1 as GeoPoint).distanceToAsDouble(p2)
        // get adjusted distance, shortened to the next lower number starting with 1, 2 or 5
        val xMetersAdjusted = if (this.adjustLength) adjustScaleBarLength(xMeters) else xMeters
        // get adjusted length in pixels
        val xBarLengthPixels = (xLen * xMetersAdjusted / xMeters).toInt()

        // Two points, yLen apart, at scale bar screen location
        p1 = projection.fromPixels(screenWidth / 2, (screenHeight / 2) - (yLen / 2), null)
        p2 = projection.fromPixels(screenWidth / 2, (screenHeight / 2) + (yLen / 2), null)

        // get distance in meters between points
        val yMeters = (p1 as GeoPoint).distanceToAsDouble(p2)
        // get adjusted distance, shortened to the next lower number starting with 1, 2 or 5
        val yMetersAdjusted = if (this.adjustLength) adjustScaleBarLength(yMeters) else yMeters
        // get adjusted length in pixels
        val yBarLengthPixels = (yLen * yMetersAdjusted / yMeters).toInt()

        // create text
        val xMsg = scaleBarLengthText(xMetersAdjusted)
        val xTextRect = Rect()
        textPaint!!.getTextBounds(xMsg, 0, xMsg.length, xTextRect)
        var xTextSpacing = (xTextRect.height() / 5.0).toInt()

        // create text
        val yMsg = scaleBarLengthText(yMetersAdjusted)
        val yTextRect = Rect()
        textPaint!!.getTextBounds(yMsg, 0, yMsg.length, yTextRect)
        var yTextSpacing = (yTextRect.height() / 5.0).toInt()
        var xTextHeight = xTextRect.height()
        var yTextHeight = yTextRect.height()

        barPath.rewind()

        //** alignBottom ad-ons
        var barOriginX = 0
        var barOriginY = 0
        var barToX = xBarLengthPixels
        var barToY = yBarLengthPixels
        if (mAlignBottom) {
            xTextSpacing *= -1
            xTextHeight *= -1
            barOriginY = this.mapHeight
            barToY = barOriginY - yBarLengthPixels
        }

        if (mAlignRight) {
            yTextSpacing *= -1
            yTextHeight *= -1
            barOriginX = this.mapWidth
            barToX = barOriginX - xBarLengthPixels
        }

        if (latitudeBar) {
            // draw latitude bar
            barPath.moveTo(barToX.toFloat(), (barOriginY + xTextHeight + xTextSpacing * 2).toFloat())
            barPath.lineTo(barToX.toFloat(), barOriginY.toFloat())
            barPath.lineTo(barOriginX.toFloat(), barOriginY.toFloat())

            if (!longitudeBar) {
                barPath.lineTo(barOriginX.toFloat(), (barOriginY + xTextHeight + xTextSpacing * 2).toFloat())
            }
            latitudeBarRect.set(barOriginX, barOriginY, barToX, barOriginY + xTextHeight + xTextSpacing * 2)
        }

        if (longitudeBar) {
            // draw longitude bar
            if (!latitudeBar) {
                barPath.moveTo((barOriginX + yTextHeight + yTextSpacing * 2).toFloat(), barOriginY.toFloat())
                barPath.lineTo(barOriginX.toFloat(), barOriginY.toFloat())
            }

            barPath.lineTo(barOriginX.toFloat(), barToY.toFloat())
            barPath.lineTo((barOriginX + yTextHeight + yTextSpacing * 2).toFloat(), barToY.toFloat())

            longitudeBarRect.set(barOriginX, barOriginY, barOriginX + yTextHeight + yTextSpacing * 2, barToY)
        }
    }

    /**
     * Returns a reduced length that starts with 1, 2 or 5 and trailing zeros. If set to nautical or
     * imperial the input will be transformed before and after the reduction so that the result
     * holds in that respective unit.
     *
     * @param length length to round
     * @return reduced, rounded (in m, nm or mi depending on setting) result
     */
    private fun adjustScaleBarLength(length: Double): Double {
        var length = length
        var pow: Long = 0
        var feet = false
        if (mUnitsOfMeasure == UnitsOfMeasure.imperial) {
            if (length >= GeoConstants.METERS_PER_STATUTE_MILE / 5) length = length / GeoConstants.METERS_PER_STATUTE_MILE
            else {
                length = length * GeoConstants.FEET_PER_METER
                feet = true
            }
        } else if (mUnitsOfMeasure == UnitsOfMeasure.nautical) {
            if (length >= GeoConstants.METERS_PER_NAUTICAL_MILE / 5) length = length / GeoConstants.METERS_PER_NAUTICAL_MILE
            else {
                length = length * GeoConstants.FEET_PER_METER
                feet = true
            }
        }

        while (length >= 10) {
            pow++
            length /= 10.0
        }
        while (length < 1 && length > 0) {
            pow--
            length *= 10.0
        }

        if (length < 2) {
            length = 1.0
        } else if (length < 5) {
            length = 2.0
        } else {
            length = 5.0
        }
        if (feet) length = length / GeoConstants.FEET_PER_METER
        else if (mUnitsOfMeasure == UnitsOfMeasure.imperial) length = length * GeoConstants.METERS_PER_STATUTE_MILE
        else if (mUnitsOfMeasure == UnitsOfMeasure.nautical) length = length * GeoConstants.METERS_PER_NAUTICAL_MILE
        length *= 10.0.pow(pow.toDouble())
        return length
    }

    protected fun scaleBarLengthText(meters: Double): String {
        when (mUnitsOfMeasure) {
            UnitsOfMeasure.metric -> if (meters >= 1000 * 5) {
                return getConvertedScaleString(meters, UnitOfMeasure.Kilometer, "%.0f")
            } else if (meters >= 1000 / 5) {
                return getConvertedScaleString(meters, UnitOfMeasure.Kilometer, "%.1f")
            } else if (meters >= 20) {
                return getConvertedScaleString(meters, UnitOfMeasure.Meter, "%.0f")
            } else {
                return getConvertedScaleString(meters, UnitOfMeasure.Meter, "%.2f")
            }

            UnitsOfMeasure.imperial -> if (meters >= GeoConstants.Companion.METERS_PER_STATUTE_MILE * 5) {
                return getConvertedScaleString(meters, UnitOfMeasure.StatuteMile, "%.0f")
            } else if (meters >= GeoConstants.Companion.METERS_PER_STATUTE_MILE / 5) {
                return getConvertedScaleString(meters, UnitOfMeasure.StatuteMile, "%.1f")
            } else {
                return getConvertedScaleString(meters, UnitOfMeasure.Foot, "%.0f")
            }

            UnitsOfMeasure.nautical -> if (meters >= GeoConstants.Companion.METERS_PER_NAUTICAL_MILE * 5) {
                return getConvertedScaleString(meters, UnitOfMeasure.NauticalMile, "%.0f")
            } else if (meters >= GeoConstants.Companion.METERS_PER_NAUTICAL_MILE / 5) {
                return getConvertedScaleString(meters, UnitOfMeasure.NauticalMile, "%.1f")
            } else {
                return getConvertedScaleString(meters, UnitOfMeasure.Foot, "%.0f")
            }

            else -> if (meters >= 1000 * 5) {
                return getConvertedScaleString(meters, UnitOfMeasure.Kilometer, "%.0f")
            } else if (meters >= 1000 / 5) {
                return getConvertedScaleString(meters, UnitOfMeasure.Kilometer, "%.1f")
            } else if (meters >= 20) {
                return getConvertedScaleString(meters, UnitOfMeasure.Meter, "%.0f")
            } else {
                return getConvertedScaleString(meters, UnitOfMeasure.Meter, "%.2f")
            }
        }
    }

    override fun onDetach(mapView: MapView?) {
        this.context = null
        this.mMapView = null
        barPaint = null
        bgPaint = null
        textPaint = null
    }

    /**
     * @since 6.0.0
     */
    private fun getConvertedScaleString(
        pMeters: Double,
        pConversion: UnitOfMeasure,
        pFormat: String
    ): String {
        return Companion.getScaleString(
            context!!,
            String.format(
                Locale.getDefault(), pFormat,
                pMeters / pConversion.conversionFactorToMeters
            ),
            pConversion
        )
    }

    private val mapWidth: Int
        /**
         * @since 6.1.0
         */
        get() = if (mMapView != null) mMapView!!.getWidth() else mMapWidth

    private val mapHeight: Int
        /**
         * @since 6.1.0
         */
        get() = if (mMapView != null) mMapView!!.getHeight() else mMapHeight

    companion object {
        // ===========================================================
        // Fields
        // ===========================================================
        private val sTextBoundsRect = Rect()

        /**
         * @since 6.1.1
         */
        fun getScaleString(
            pContext: Context,
            pValue: String?,
            pUnitOfMeasure: UnitOfMeasure
        ): String {
            return pContext.getString(
                R.string.format_distance_value_unit,
                pValue, pContext.getString(pUnitOfMeasure.stringResId)
            )
        }
    }
}

package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants.UnitOfMeasure
import org.osmdroid.views.Projection
import java.util.Locale

/**
 * ScaleDiskOverlay displays a disk of a given radius (distance) around a GeoPoint
 *
 * @author Fabrice Fontaine
 * @since 6.1.1
 */
class ScaleDiskOverlay(
    pContext: Context,
    pGeoCenter: GeoPoint,
    pValue: Int, pUnitOfMeasure: UnitOfMeasure
) : Overlay() {
    private val mPixelCenter = Point()
    private val mLabelRect = Rect()

    private val mGeoCenter: GeoPoint
    private val mMeters: Double
    private val mLabel: String

    private var mCirclePaint1: Paint? = null
    private var mCirclePaint2: Paint? = null
    private var mTextPaint: Paint? = null

    private var mLabelOffsetTop: Int? = null
    private var mLabelOffsetBottom: Int? = null
    private var mLabelOffsetLeft: Int? = null
    private var mLabelOffsetRight: Int? = null

    private var mDisplaySizeMin = 0
    private var mDisplaySizeMax = 0

    init {
        mGeoCenter = pGeoCenter
        mMeters = pValue * pUnitOfMeasure.conversionFactorToMeters
        mLabel = ScaleBarOverlay.Companion.getScaleString(
            pContext,
            String.format(Locale.getDefault(), "%d", pValue), pUnitOfMeasure
        )
    }

    /**
     * Circle Paint 1 setter (typically for a disk)
     * Can be null; will be used before Circle Paint 2
     */
    fun setCirclePaint1(pPaint: Paint?) {
        mCirclePaint1 = pPaint
    }

    /**
     * Circle Paint 2 setter (typically for a circle)
     * Can be null; will be used after Circle Paint 1
     */
    fun setCirclePaint2(pPaint: Paint?) {
        mCirclePaint2 = pPaint
    }

    /**
     * Label Paint setter (null means no label will be displayed)
     */
    fun setTextPaint(pPaint: Paint?) {
        mTextPaint = pPaint
    }

    /**
     * Label offset setter for top (null means no label on top)
     */
    fun setLabelOffsetTop(pValue: Int?) {
        mLabelOffsetTop = pValue
    }

    /**
     * Label offset setter for bottom (null means no label on bottom)
     */
    fun setLabelOffsetBottom(pValue: Int?) {
        mLabelOffsetBottom = pValue
    }

    /**
     * Label offset setter for left (null means no label on left)
     */
    fun setLabelOffsetLeft(pValue: Int?) {
        mLabelOffsetLeft = pValue
    }

    /**
     * Label offset setter for right (null means no label on right)
     */
    fun setLabelOffsetRight(pValue: Int?) {
        mLabelOffsetRight = pValue
    }

    /**
     * Minimum display size setter (<= 0 means no minimum)
     */
    fun setDisplaySizeMin(pValue: Int) {
        mDisplaySizeMin = pValue
    }

    /**
     * Maximum display size setter (<= 0 means no maximum)
     */
    fun setDisplaySizeMax(pValue: Int) {
        mDisplaySizeMax = pValue
    }

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        pProjection.toPixels(mGeoCenter, mPixelCenter)
        val x = mPixelCenter.x
        val y = mPixelCenter.y
        val radius = pProjection.metersToPixels(
            mMeters.toFloat(), mGeoCenter.latitude, pProjection.zoomLevel
        ).toInt()
        if (mDisplaySizeMin > 0 && 2 * radius < mDisplaySizeMin) {
            return
        }
        if (mDisplaySizeMax > 0 && 2 * radius > mDisplaySizeMax) {
            return
        }
        if (mCirclePaint1 != null) {
            pCanvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), mCirclePaint1!!)
        }
        if (mCirclePaint2 != null) {
            pCanvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), mCirclePaint2!!)
        }
        if (mTextPaint != null) {
            mTextPaint!!.getTextBounds(mLabel, 0, mLabel.length, mLabelRect)
            if (mLabelOffsetTop != null) {
                val offsetX = this.offsetX
                val offsetY = -radius + getOffsetY(mLabelOffsetTop!!)
                pCanvas.drawText(mLabel, (x + offsetX).toFloat(), (y + offsetY).toFloat(), mTextPaint!!)
            }
            if (mLabelOffsetLeft != null) {
                val offsetX = -radius + getOffsetX(mLabelOffsetLeft!!)
                val offsetY = this.offsetY
                pCanvas.drawText(mLabel, (x + offsetX).toFloat(), (y + offsetY).toFloat(), mTextPaint!!)
            }
            if (mLabelOffsetBottom != null) {
                val offsetX = this.offsetX
                val offsetY = radius + getOffsetY(mLabelOffsetBottom!!)
                pCanvas.drawText(mLabel, (x + offsetX).toFloat(), (y + offsetY).toFloat(), mTextPaint!!)
            }
            if (mLabelOffsetRight != null) {
                val offsetX = radius + getOffsetX(mLabelOffsetRight!!)
                val offsetY = this.offsetY
                pCanvas.drawText(mLabel, (x + offsetX).toFloat(), (y + offsetY).toFloat(), mTextPaint!!)
            }
        }
    }

    private val offsetX: Int
        get() = -mLabelRect.width() / 2

    private val offsetY: Int
        get() = 0

    private fun getOffsetX(pOffsetX: Int): Int {
        return pOffsetX + (if (pOffsetX >= 0) 0 else -mLabelRect.width())
    }

    private fun getOffsetY(pOffsetY: Int): Int {
        return pOffsetY + (if (pOffsetY >= 0) -mLabelRect.top else -mLabelRect.bottom)
    }
}

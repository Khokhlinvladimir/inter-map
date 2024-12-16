package org.osmdroid.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import org.osmdroid.R

class CustomZoomButtonsDisplay(private val mMapView: MapView) {
    enum class HorizontalPosition {
        LEFT, CENTER, RIGHT
    }

    enum class VerticalPosition {
        TOP, CENTER, BOTTOM
    }

    private val mUnrotatedPoint = Point()
    private var mZoomInBitmapEnabled: Bitmap? = null
    private var mZoomOutBitmapEnabled: Bitmap? = null
    private var mZoomInBitmapDisabled: Bitmap? = null
    private var mZoomOutBitmapDisabled: Bitmap? = null
    private var mAlphaPaint: Paint? = null
    private var mBitmapSize = 0
    private var mHorizontalPosition: HorizontalPosition? = null
    private var mVerticalPosition: VerticalPosition? = null
    private var mHorizontalOrVertical = false
    private var mMargin // as fraction of the bitmap size
            = 0f
    private var mPadding // as fraction of the bitmap size
            = 0f
    private var mAdditionalPixelMarginLeft // additional left margin in pixels
            = 0f
    private var mAdditionalPixelMarginTop // additional top margin in pixels
            = 0f
    private var mAdditionalPixelMarginRight // additional right margin in pixels
            = 0f
    private var mAdditionalPixelMarginBottom // additional bottom margin in pixels
            = 0f
    private var mPixelMarginLeft // calculated overall left margin in pixels
            = 0f
    private var mPixelMarginTop // calculated overall top margin in pixels
            = 0f
    private var mPixelMarginRight // calculated overall right margin in pixels
            = 0f
    private var mPixelMarginBottom // calculated overall bottom margin in pixels
            = 0f

    init {
        // default values
        setPositions(true, HorizontalPosition.CENTER, VerticalPosition.BOTTOM)
        setMarginPadding(.5f, .5f)
    }

    fun setPositions(
            pHorizontalOrVertical: Boolean,
            pHorizontalPosition: HorizontalPosition?, pVerticalPosition: VerticalPosition?) {
        mHorizontalOrVertical = pHorizontalOrVertical
        mHorizontalPosition = pHorizontalPosition
        mVerticalPosition = pVerticalPosition
    }

    /**
     * sets margin and padding as fraction of the bitmap width
     */
    fun setMarginPadding(pMargin: Float, pPadding: Float) {
        mMargin = pMargin
        mPadding = pPadding
        refreshPixelMargins()
    }

    /**
     * sets additional margin in pixels
     */
    fun setAdditionalPixelMargins(pLeft: Float, pTop: Float, pRight: Float, pBottom: Float) {
        mAdditionalPixelMarginLeft = pLeft
        mAdditionalPixelMarginTop = pTop
        mAdditionalPixelMarginRight = pRight
        mAdditionalPixelMarginBottom = pBottom
        refreshPixelMargins()
    }

    /**
     * calculate overall margins in pixels
     */
    private fun refreshPixelMargins() {
        val bitmapFractionMarginInPixels = mMargin * mBitmapSize
        mPixelMarginLeft = bitmapFractionMarginInPixels + mAdditionalPixelMarginLeft
        mPixelMarginTop = bitmapFractionMarginInPixels + mAdditionalPixelMarginTop
        mPixelMarginRight = bitmapFractionMarginInPixels + mAdditionalPixelMarginRight
        mPixelMarginBottom = bitmapFractionMarginInPixels + mAdditionalPixelMarginBottom
    }

    fun setBitmaps(pInEnabled: Bitmap?, pInDisabled: Bitmap?,
                   pOutEnabled: Bitmap?, pOutDisabled: Bitmap?) {
        mZoomInBitmapEnabled = pInEnabled
        mZoomInBitmapDisabled = pInDisabled
        mZoomOutBitmapEnabled = pOutEnabled
        mZoomOutBitmapDisabled = pOutDisabled
        mBitmapSize = mZoomInBitmapEnabled!!.width
        refreshPixelMargins()
    }

    protected fun getZoomBitmap(pInOrOut: Boolean, pEnabled: Boolean): Bitmap {
        val icon = getIcon(pInOrOut)
        mBitmapSize = icon.width
        refreshPixelMargins()
        val bitmap = Bitmap.createBitmap(mBitmapSize, mBitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint()
        backgroundPaint.color = if (pEnabled) Color.WHITE else Color.LTGRAY
        backgroundPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, (mBitmapSize - 1).toFloat(), (mBitmapSize - 1).toFloat(), backgroundPaint)
        canvas.drawBitmap(icon, 0f, 0f, null)
        return bitmap
    }

    protected fun getIcon(pInOrOut: Boolean): Bitmap {
        val resourceId = if (pInOrOut) R.drawable.sharp_add_black_36 else R.drawable.sharp_remove_black_36
        return (mMapView.resources.getDrawable(resourceId) as BitmapDrawable).bitmap
    }

    fun draw(pCanvas: Canvas, pAlpha01: Float,
             pZoomInEnabled: Boolean, pZoomOutEnabled: Boolean) {
        if (pAlpha01 == 0f) {
            return
        }
        val paint: Paint?
        if (pAlpha01 == 1f) {
            paint = null
        } else {
            if (mAlphaPaint == null) {
                mAlphaPaint = Paint()
            }
            mAlphaPaint!!.alpha = (pAlpha01 * 255).toInt()
            paint = mAlphaPaint
        }
        pCanvas.drawBitmap(
                getBitmap(true, pZoomInEnabled)!!,
                getTopLeft(pInOrOut = true, pXOrY = true),
                getTopLeft(pInOrOut = true, pXOrY = false),
                paint)
        pCanvas.drawBitmap(
                getBitmap(false, pZoomOutEnabled)!!,
                getTopLeft(pInOrOut = false, pXOrY = true),
                getTopLeft(pInOrOut = false, pXOrY = false),
                paint)
    }

    private fun getTopLeft(pInOrOut: Boolean, pXOrY: Boolean): Float {
        val topLeft: Float
        if (pXOrY) {
            topLeft = getFirstLeft(mMapView.width)
            if (!mHorizontalOrVertical) { // vertical: same left
                return topLeft
            }
            return if (!pInOrOut) { // horizontal: zoom out first
                topLeft
            } else topLeft + mBitmapSize + mPadding * mBitmapSize
        }
        topLeft = getFirstTop(mMapView.height)
        if (mHorizontalOrVertical) { // horizontal: same top
            return topLeft
        }
        return if (pInOrOut) { // vertical: zoom in first
            topLeft
        } else topLeft + mBitmapSize + mPadding * mBitmapSize
    }

    private fun getFirstLeft(pMapViewWidth: Int): Float {
        when (mHorizontalPosition) {
            HorizontalPosition.LEFT -> return mPixelMarginLeft
            HorizontalPosition.RIGHT -> return (pMapViewWidth - mPixelMarginRight - mBitmapSize
                    - if (mHorizontalOrVertical) (mPadding * mBitmapSize + mBitmapSize).toInt() else 0)

            HorizontalPosition.CENTER -> return (pMapViewWidth / 2f
                    - if (mHorizontalOrVertical) mPadding * mBitmapSize / 2 + mBitmapSize else mBitmapSize / 2f)

            else -> {}
        }
        throw IllegalArgumentException()
    }

    private fun getFirstTop(pMapViewHeight: Int): Float {
        when (mVerticalPosition) {
            VerticalPosition.TOP -> return mPixelMarginTop
            VerticalPosition.BOTTOM -> return (pMapViewHeight - mPixelMarginBottom - mBitmapSize
                    - if (mHorizontalOrVertical) 0 else (mPadding * mBitmapSize + mBitmapSize).toInt())

            VerticalPosition.CENTER -> return (pMapViewHeight / 2f
                    - if (mHorizontalOrVertical) mBitmapSize / 2f else mPadding * mBitmapSize / 2 + mBitmapSize)

            else -> {}
        }
        throw IllegalArgumentException()
    }

    private fun getBitmap(pInOrOut: Boolean, pEnabled: Boolean): Bitmap? {
        if (mZoomInBitmapEnabled == null) {
            setBitmaps(
                    getZoomBitmap(pInOrOut = true, pEnabled = true),
                    getZoomBitmap(pInOrOut = true, pEnabled = false),
                    getZoomBitmap(pInOrOut = false, pEnabled = true),
                    getZoomBitmap(pInOrOut = false, pEnabled = false)
            )
        }
        if (pInOrOut) {
            return if (pEnabled) mZoomInBitmapEnabled else mZoomInBitmapDisabled
        }
        return if (pEnabled) mZoomOutBitmapEnabled else mZoomOutBitmapDisabled
    }

    @Deprecated("")
    fun isTouchedRotated(pMotionEvent: MotionEvent, pInOrOut: Boolean): Boolean {
        if (mMapView.getMapOrientation() == 0f) {
            mUnrotatedPoint[pMotionEvent.x.toInt()] = pMotionEvent.y.toInt()
        } else {
            mMapView.projection.rotateAndScalePoint(pMotionEvent.x.toInt(), pMotionEvent.y.toInt(), mUnrotatedPoint)
        }
        return isTouched(mUnrotatedPoint.x, mUnrotatedPoint.y, pInOrOut)
    }

    fun isTouched(pMotionEvent: MotionEvent, pInOrOut: Boolean): Boolean {
        return if (pMotionEvent.action == MotionEvent.ACTION_UP) {
            isTouched(pMotionEvent.x.toInt(), pMotionEvent.y.toInt(), pInOrOut)
        } else {
            false
        }
    }

    private fun isTouched(pEventX: Int, pEventY: Int, pInOrOut: Boolean): Boolean {
        return (isTouched(pInOrOut, true, pEventX.toFloat())
                && isTouched(pInOrOut, false, pEventY.toFloat()))
    }

    private fun isTouched(pInOrOut: Boolean, pXOrY: Boolean, pEvent: Float): Boolean {
        val topLeft = getTopLeft(pInOrOut, pXOrY)
        return pEvent >= topLeft && pEvent <= topLeft + mBitmapSize
    }
}
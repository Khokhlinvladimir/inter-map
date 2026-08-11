package org.osmdroid.views.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection

/**
 * Place an image on the map, each corner (4) of the image being associated with a [GeoPoint]
 * or only top-left and bottom-right corners
 *
 * @author Fabrice Fontaine
 * Triggered by issue 1361 (https://github.com/osmdroid/osmdroid/issues/1361)
 * Inspired by [GroundOverlay2] and [GroundOverlay4]
 * @since 6.1.1
 */
class GroundOverlay : Overlay() {
    private val mPaint = Paint()
    private val mMatrix = Matrix()

    var bearing: Float = 0.0f
    private var mTransparency = 0f
    private var mImage: Bitmap? = null

    private var mMatrixSrc: FloatArray? = null
    private var mMatrixDst: FloatArray? = null

    private var mTopLeft: GeoPoint? = null
    var topRight: GeoPoint? = null
        private set
    private var mBottomRight: GeoPoint? = null
    var bottomLeft: GeoPoint? = null
        private set

    var image: Bitmap?
        get() = mImage
        set(pImage) {
            mImage = pImage
            mMatrixSrc = null
        }

    var transparency: Float
        get() = mTransparency
        set(pTransparency) {
            mTransparency = pTransparency
            mPaint.setAlpha(255 - (mTransparency * 255).toInt())
        }

    val topLeft: GeoPoint?
        get() = mTopLeft

    val bottomRight: GeoPoint?
        get() = mBottomRight

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        if (mImage == null) {
            return
        }
        computeMatrix(pProjection)
        pCanvas.drawBitmap(mImage!!, mMatrix, mPaint)
    }

    fun setPosition(
        pTopLeft: GeoPoint, pTopRight: GeoPoint,
        pBottomRight: GeoPoint, pBottomLeft: GeoPoint
    ) {
        mMatrix.reset()
        mTopLeft = GeoPoint(pTopLeft)
        this.topRight = GeoPoint(pTopRight)
        mBottomRight = GeoPoint(pBottomRight)
        this.bottomLeft = GeoPoint(pBottomLeft)
        mBounds = BoundingBox(
            pTopLeft.latitude, pTopRight.longitude,
            pBottomRight.latitude, pTopLeft.longitude
        )
    }

    fun setPosition(pTopLeft: GeoPoint, pBottomRight: GeoPoint) {
        mMatrix.reset()
        mMatrixSrc = null
        mMatrixDst = null
        mTopLeft = GeoPoint(pTopLeft)
        this.topRight = null
        mBottomRight = GeoPoint(pBottomRight)
        this.bottomLeft = null
        mBounds = BoundingBox(
            pTopLeft.latitude, pBottomRight.longitude,
            pBottomRight.latitude, pTopLeft.longitude
        )
    }

    // TODO check if performance-wise it would make sense to use the mMatrix.setPolyToPoly option
    // TODO even for the 2 corner case
    private fun computeMatrix(pProjection: Projection) {
        if (this.topRight == null) { // only 2 corners
            val x0 = pProjection.getLongPixelXFromLongitude(mTopLeft!!.longitude)
            val y0 = pProjection.getLongPixelYFromLatitude(mTopLeft!!.latitude)
            val x1 = pProjection.getLongPixelXFromLongitude(mBottomRight!!.longitude)
            val y1 = pProjection.getLongPixelYFromLatitude(mBottomRight!!.latitude)
            val widthOnTheMap = (x1 - x0).toFloat()
            val heightOnTheMap = (y1 - y0).toFloat()
            val scaleX = widthOnTheMap / mImage!!.getWidth()
            val scaleY = heightOnTheMap / mImage!!.getHeight()
            mMatrix.setScale(scaleX, scaleY)
            mMatrix.postTranslate(x0.toFloat(), y0.toFloat())
            return
        }
        // 4 corners
        if (mMatrixSrc == null) {
            mMatrixSrc = FloatArray(8)
            val width = mImage!!.getWidth()
            val height = mImage!!.getHeight()
            mMatrixSrc!![0] = 0f
            mMatrixSrc!![1] = 0f
            mMatrixSrc!![2] = width.toFloat()
            mMatrixSrc!![3] = 0f
            mMatrixSrc!![4] = width.toFloat()
            mMatrixSrc!![5] = height.toFloat()
            mMatrixSrc!![6] = 0f
            mMatrixSrc!![7] = height.toFloat()
        }
        if (mMatrixDst == null) {
            mMatrixDst = FloatArray(8)
        }
        val topLeftCornerX = pProjection.getLongPixelXFromLongitude(mTopLeft!!.longitude)
        val topLeftCornerY = pProjection.getLongPixelYFromLatitude(mTopLeft!!.latitude)
        val topRightCornerX = pProjection.getLongPixelXFromLongitude(topRight!!.longitude)
        val topRightCornerY = pProjection.getLongPixelYFromLatitude(topRight!!.latitude)
        val bottomRightCornerX = pProjection.getLongPixelXFromLongitude(mBottomRight!!.longitude)
        val bottomRightCornerY = pProjection.getLongPixelYFromLatitude(mBottomRight!!.latitude)
        val bottomLeftCornerX = pProjection.getLongPixelXFromLongitude(bottomLeft!!.longitude)
        val bottomLeftCornerY = pProjection.getLongPixelYFromLatitude(bottomLeft!!.latitude)
        mMatrixDst!![0] = topLeftCornerX.toFloat()
        mMatrixDst!![1] = topLeftCornerY.toFloat()
        mMatrixDst!![2] = topRightCornerX.toFloat()
        mMatrixDst!![3] = topRightCornerY.toFloat()
        mMatrixDst!![4] = bottomRightCornerX.toFloat()
        mMatrixDst!![5] = bottomRightCornerY.toFloat()
        mMatrixDst!![6] = bottomLeftCornerX.toFloat()
        mMatrixDst!![7] = bottomLeftCornerY.toFloat()

        mMatrix.setPolyToPoly(mMatrixSrc, 0, mMatrixDst, 0, 4)
    }
}

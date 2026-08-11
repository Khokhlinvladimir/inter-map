package org.osmdroid.views.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection

/**
 * Place an image on the map, each corner of the image being associated with a [GeoPoint]
 *
 * @author Fabrice Fontaine
 * Triggered by issue 1361 (https://github.com/osmdroid/osmdroid/issues/1361)
 * Inspired by [GroundOverlay2]
 * @since 6.1.1
 */
@Deprecated("Use {@link GroundOverlay} instead")
class GroundOverlay4 : Overlay() {
    protected val paint: Paint = Paint()
    protected val matrix: Matrix = Matrix()

    var bearing: Float = 0.0f
    protected var mTransparency: Float = 0f
    private var mImage: Bitmap? = null

    var image: Bitmap?
        get() = mImage
        set(pImage) {
            mImage = pImage
            val image = mImage
            if (image == null) {
                return
            }
            val width: Int = image.width
            val height: Int = image.height
            mMatrixSrc[0] = 0f
            mMatrixSrc[1] = 0f
            mMatrixSrc[2] = width.toFloat()
            mMatrixSrc[3] = 0f
            mMatrixSrc[4] = width.toFloat()
            mMatrixSrc[5] = height.toFloat()
            mMatrixSrc[6] = 0f
            mMatrixSrc[7] = height.toFloat()
        }

    var transparency: Float
        get() = mTransparency
        set(pTransparency) {
            mTransparency = pTransparency
            paint.setAlpha(255 - (mTransparency * 255).toInt())
        }

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        if (mImage == null) {
            return
        }
        computeMatrix(pProjection)
        pCanvas.drawBitmap(mImage!!, this.matrix, this.paint)
    }

    private val mMatrixSrc = FloatArray(8)
    private val mMatrixDst = FloatArray(8)

    private var mTopLeft: GeoPoint? = null
    private var mTopRight: GeoPoint? = null
    private var mBottomRight: GeoPoint? = null
    private var mBottomLeft: GeoPoint? = null

    fun setPosition(
        pTopLeft: GeoPoint, pTopRight: GeoPoint,
        pBottomRight: GeoPoint, pBottomLeft: GeoPoint
    ) {
        mTopLeft = GeoPoint(pTopLeft)
        mTopRight = GeoPoint(pTopRight)
        mBottomRight = GeoPoint(pBottomRight)
        mBottomLeft = GeoPoint(pBottomLeft)
    }

    protected fun computeMatrix(pProjection: Projection) {
        val topLeftCornerX = pProjection.getLongPixelXFromLongitude(mTopLeft!!.longitude)
        val topLeftCornerY = pProjection.getLongPixelYFromLatitude(mTopLeft!!.latitude)
        val topRightCornerX = pProjection.getLongPixelXFromLongitude(mTopRight!!.longitude)
        val topRightCornerY = pProjection.getLongPixelYFromLatitude(mTopRight!!.latitude)
        val bottomRightCornerX = pProjection.getLongPixelXFromLongitude(mBottomRight!!.longitude)
        val bottomRightCornerY = pProjection.getLongPixelYFromLatitude(mBottomRight!!.latitude)
        val bottomLeftCornerX = pProjection.getLongPixelXFromLongitude(mBottomLeft!!.longitude)
        val bottomLeftCornerY = pProjection.getLongPixelYFromLatitude(mBottomLeft!!.latitude)

        mMatrixDst[0] = topLeftCornerX.toFloat()
        mMatrixDst[1] = topLeftCornerY.toFloat()
        mMatrixDst[2] = topRightCornerX.toFloat()
        mMatrixDst[3] = topRightCornerY.toFloat()
        mMatrixDst[4] = bottomRightCornerX.toFloat()
        mMatrixDst[5] = bottomRightCornerY.toFloat()
        mMatrixDst[6] = bottomLeftCornerX.toFloat()
        mMatrixDst[7] = bottomLeftCornerY.toFloat()

        this.matrix.setPolyToPoly(mMatrixSrc, 0, mMatrixDst, 0, 4)
    }
}

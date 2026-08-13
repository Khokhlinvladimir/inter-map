package org.osmdroid.views.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection

/**
 * A ground overlay is an image that is fixed to 2 corners on a map using simple scaling
 * that does not take into consideration the curvature of the Earth.
 *
 * @author pasniak inspired by zkhan's code in Avare
 * @since 6.0.0
 */
@Deprecated("Use {@link GroundOverlay} instead")
class GroundOverlay2 : Overlay() {
    protected val paint: Paint = Paint()
    protected val matrix: Matrix = Matrix()

    var bearing: Float = 0.0f
    protected var mTransparency: Float = 0f
    var image: Bitmap? = null

    var transparency: Float
        get() = mTransparency
        set(pTransparency) {
            mTransparency = pTransparency
            paint.setAlpha(255 - (mTransparency * 255).toInt())
        }

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        if (this.image == null) {
            return
        }
        computeMatrix(pProjection)
        pCanvas.drawBitmap(this.image!!, this.matrix, this.paint)
    }

    private var mLonL = 0f
    private var mLatU = 0f
    private var mLonR = 0f
    private var mLatD = 0f

    /**
     * @param UL upper left
     * @param RD lower right
     */
    fun setPosition(UL: GeoPoint, RD: GeoPoint) {
        mLatU = UL.latitude.toFloat()
        mLonL = UL.longitude.toFloat()
        mLatD = RD.latitude.toFloat()
        mLonR = RD.longitude.toFloat()
    }

    protected fun computeMatrix(pProjection: Projection) {
        val x0 = pProjection.getLongPixelXFromLongitude(mLonL.toDouble())
        val y0 = pProjection.getLongPixelYFromLatitude(mLatU.toDouble())
        val x1 = pProjection.getLongPixelXFromLongitude(mLonR.toDouble())
        val y1 = pProjection.getLongPixelYFromLatitude(mLatD.toDouble())

        val widthOnTheMap = (x1 - x0).toFloat()
        val heightOnTheMap = (y1 - y0).toFloat()

        val scaleX = widthOnTheMap / this.image!!.getWidth()
        val scaleY = heightOnTheMap / this.image!!.getHeight()

        this.matrix.setScale(scaleX, scaleY)
        this.matrix.postTranslate(x0.toFloat(), y0.toFloat())
    }
}

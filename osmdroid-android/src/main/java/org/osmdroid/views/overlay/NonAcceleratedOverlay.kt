package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.views.MapView

/**
 * This will allow an [Overlay] that is not HW acceleration compatible to work in a HW
 * accelerated MapView. It will create a screen-sized backing Bitmap for the Overlay to draw to and
 * then will draw the Bitmap to the HW accelerated canvas. Due to the extra work, it does not draw
 * the shadow layer. If the Canvas passed into the Overlay is not HW accelerated or if
 * [.isUsingBackingBitmap] returns false then it draws normally (including the shadow layer)
 * without the backing Bitmap. <br></br>
 * <br></br>
 * TODO:
 *
 *  1. Implement a flag to determine if the drawing has actually changed. If not, then reuse the
 * last frame's backing bitmap. This will prevent having to re-upload the bitmap texture to GPU.
 *
 */
abstract class NonAcceleratedOverlay : Overlay {
    private var mBackingBitmap: Bitmap? = null
    private var mBackingCanvas: Canvas? = null
    private val mBackingMatrix = Matrix()
    private val mCanvasIdentityMatrix = Matrix()

    /**
     * A delegate for [.draw].
     */
    protected abstract fun onDraw(c: Canvas?, osmv: MapView?, shadow: Boolean)

    /**
     * Use [.NonAcceleratedOverlay] instead
     */
    @Deprecated("")
    constructor(ctx: Context?) : super(ctx)

    constructor() : super()


    /**
     * Override if you really want access to the original (possibly) accelerated canvas.
     */
    protected fun onDraw(c: Canvas?, acceleratedCanvas: Canvas?, osmv: MapView?, shadow: Boolean) {
        onDraw(c, osmv, shadow)
    }

    val isUsingBackingBitmap: Boolean
        /**
         * Allow forcing this overlay to skip drawing using backing Bitmap by returning false.
         */
        get() = true

    override fun onDetach(mapView: MapView?) {
        mBackingBitmap = null
        mBackingCanvas = null
        super.onDetach(mapView)
    }

    override fun draw(c: Canvas, osmv: MapView, shadow: Boolean) {
        // First check to see if we want to use the backing bitmap
        val atLeastHoneycomb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
        if (this.isUsingBackingBitmap && atLeastHoneycomb && c.isHardwareAccelerated()) {
            // Drawing a shadow layer would require a second backing Bitmap due to the way HW
            // accelerated drawBitmap works. One could extend this Overlay to implement that if
            // needed.
            if (shadow) return

            // If we don't have any drawing area, then don't draw
            if (c.getWidth() == 0 || c.getHeight() == 0) return

            if (mBackingBitmap == null || mBackingBitmap!!.getWidth() != c.getWidth() || mBackingBitmap!!.getHeight() != c.getHeight()) {
                mBackingBitmap = null
                mBackingCanvas = null
                try {
                    mBackingBitmap = Bitmap.createBitmap(
                        c.getWidth(), c.getHeight(),
                        Bitmap.Config.ARGB_8888
                    )
                } catch (e: OutOfMemoryError) {
                    Log.e(IMapView.LOGTAG, "OutOfMemoryError creating backing bitmap in NonAcceleratedOverlay.")
                    System.gc()
                    return
                }

                mBackingCanvas = Canvas(mBackingBitmap!!)
            }

            mBackingCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            c.getMatrix(mBackingMatrix)
            mBackingCanvas!!.setMatrix(mBackingMatrix)
            onDraw(mBackingCanvas, c, osmv, shadow)
            c.save()
            c.getMatrix(mCanvasIdentityMatrix)
            mCanvasIdentityMatrix.invert(mCanvasIdentityMatrix)
            c.concat(mCanvasIdentityMatrix)
            c.drawBitmap(mBackingBitmap!!, 0f, 0f, null)
            c.restore()
        } else onDraw(c, c, osmv, shadow)
    }
}

package org.osmdroid.views.overlay

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection

/**
 * [IconOverlay] is an icon placed at a particular
 * [IGeoPoint] on the [MapView]'s surface.
 *
 *
 * Inspired by [Marker] but without the Datafields and the pop-window support.
 *
 *
 * Created by k3b on 16.07.2015.
 */
open class IconOverlay : Overlay {
    /*attributes for standard features:*/
    protected var mIcon: Drawable? = null
    protected var mPosition: IGeoPoint? = null

    protected var mBearing: Float = 0.0f
    protected var mAnchorU: Float = ANCHOR_CENTER
    protected var mAnchorV: Float = ANCHOR_CENTER
    protected var mAlpha: Float = 1.0f //opaque

    protected var mFlat: Boolean = false //billboard;

    protected var mPositionPixels: Point = Point()

    /**
     * save to be called in non-gui-thread
     */
    constructor()

    /**
     * save to be called in non-gui-thread
     */
    constructor(position: IGeoPoint?, icon: Drawable?) {
        set(position, icon)
    }

    /**
     * Draw the icon.
     */
    override fun draw(canvas: Canvas, pj: Projection) {
        if (mIcon == null) return
        if (mPosition == null) return

        pj.toPixels(mPosition, mPositionPixels)
        val icon = mIcon ?: return
        val width = icon.intrinsicWidth
        val height = icon.intrinsicHeight
        val rect = Rect(0, 0, width, height)
        rect.offset(-(mAnchorU * width).toInt(), -(mAnchorV * height).toInt())
        icon.bounds = rect

        icon.alpha = (mAlpha * 255).toInt()

        val rotationOnScreen = if (mFlat) -mBearing else pj.orientation - mBearing
        drawAt(canvas, icon, mPositionPixels.x, mPositionPixels.y, false, rotationOnScreen)
    }

    fun getPosition(): IGeoPoint? = mPosition

    fun set(position: IGeoPoint?, icon: Drawable?): IconOverlay {
        mPosition = position
        mIcon = icon
        return this
    }

    fun moveTo(event: MotionEvent, mapView: MapView): IconOverlay {
        val pj = mapView.projection
        moveTo(pj.fromPixels(event.getX().toInt(), event.getY().toInt()), mapView)
        return this
    }

    fun moveTo(position: IGeoPoint?, mapView: MapView): IconOverlay {
        mPosition = position
        mapView.invalidate()
        return this
    }

    companion object {
        /**
         * Usual values in the (U,V) coordinates system of the icon image
         */
        const val ANCHOR_CENTER: Float = 0.5f
        const val ANCHOR_LEFT: Float = 0.0f
        const val ANCHOR_TOP: Float = 0.0f
        const val ANCHOR_RIGHT: Float = 1.0f
        const val ANCHOR_BOTTOM: Float = 1.0f
    }
}

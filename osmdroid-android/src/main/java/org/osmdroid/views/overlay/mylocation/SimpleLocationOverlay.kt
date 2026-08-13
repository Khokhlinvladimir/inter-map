// Created by plusminus on 22:01:11 - 29.09.2008
package org.osmdroid.views.overlay.mylocation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.library.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

/**
 * @author Nicolas Gramlich
 */
class SimpleLocationOverlay(protected var PERSON_ICON: Bitmap) : Overlay() {
    protected val mPaint: Paint = Paint()

    /**
     * Coordinates the feet of the person are located.
     */
    protected var PERSON_HOTSPOT: Point = Point(24, 39)

    var myLocation: GeoPoint? = null
        protected set
    private val screenCoords = Point()

    /**
     * Use [SimpleLocationOverlay][.SimpleLocationOverlay](((BitmapDrawable)ctx.getResources().getDrawable(R.drawable.person)).getBitmap()) instead.
     */
    @Deprecated("")
    constructor(ctx: Context) : this((ctx.getResources().getDrawable(R.drawable.person) as BitmapDrawable).getBitmap())

    fun setLocation(mp: GeoPoint?) {
        this.myLocation = mp
    }

    override fun onDetach(mapView: MapView?) {
        //https://github.com/osmdroid/osmdroid/issues/477
        //commented out to prevent issues
        //this.PERSON_ICON.recycle();
    }

    override fun draw(c: Canvas, pj: Projection) {
        if (this.myLocation != null) {
            pj.toPixels(this.myLocation, screenCoords)

            c.drawBitmap(
                PERSON_ICON, (screenCoords.x - PERSON_HOTSPOT.x).toFloat(), (screenCoords.y
                        - PERSON_HOTSPOT.y).toFloat(), this.mPaint
            )
        }
    }

    /**
     * Coordinates the feet of the person are located.
     */
    fun setPersonIcon(bmp: Bitmap, hotspot: Point) {
        this.PERSON_ICON = bmp
        this.PERSON_HOTSPOT = hotspot
    }
}

package org.osmdroid.bugtestfragments

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

/**
 * See https://github.com/osmdroid/osmdroid/issues/82#issuecomment-229413838
 *
 *
 * Created by alex on 6/29/16.
 */
class Bug82WinDeath : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Bug #82 WinDeath"

    override fun addOverlays() {
        //
        val overlay = MapOverlay()
        overlay.setEnabled(true)
        mMapView!!.getOverlayManager().add(overlay)
        mMapView!!.controller!!.setCenter(GeoPoint(50.71838, -103.42443))
        mMapView!!.controller!!.setZoom(17)
    }


    class MapOverlay : Overlay() {
        private val innerPaint: Paint


        init {
            this.innerPaint = Paint()
            this.innerPaint.setColor(Color.argb(0x80, 0x43, 0x24, 0xa0))
            this.innerPaint.setStrokeWidth(2.0f)
            this.innerPaint.setStyle(Paint.Style.FILL)
        }

        override fun draw(canvas: Canvas, pProjection: Projection) {
            Log.i(TAG, "Drawing Bug82 Windeath circle")
            val point = pProjection.toPixels(GeoPoint(50.71838, -103.42443), Point())
            canvas.drawCircle(point!!.x.toFloat(), point.y.toFloat(), 100.0f, innerPaint)
        }
    }
}

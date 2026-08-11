package org.osmdroid.samplefragments.events

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.osmdroid.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.util.Timer
import java.util.TimerTask

/**
 * @author Fabrice Fontaine
 * @since 6.1.1
 */
class SampleMapCenterOffset : SampleMapEventListener() {
    private val mOffsetX = 0
    private val mOffsetY = 200
    private val mPaint = Paint()

    private var mIndex = 0
    private var t: Timer? = Timer()
    private var alive = true
    private val mList: MutableList<GeoPoint> = ArrayList<GeoPoint>()

    override val sampleTitle: String
        get() = "Animate To with Map Center Offset"

    public override fun addOverlays() {
        super.addOverlays()

        val drawable = getResources().getDrawable(R.drawable.marker_default)

        mList.add(GeoPoint(38.8977, -77.0365)) // white house
        mList.add(GeoPoint(38.8719, -77.0563)) // pentagon
        mList.add(GeoPoint(38.8895, -77.0353)) // washington monument

        for (geoPoint in mList) {
            val startMarker = Marker(mMapView!!)
            startMarker.position = geoPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.icon = drawable
            mMapView!!.getOverlays()!!.add(startMarker)
        }

        mPaint.setColor(Color.RED)
        mPaint.setStrokeWidth(5f)

        mMapView!!.getOverlays()!!.add(object : Overlay() {
            override fun draw(pCanvas: Canvas, pProjection: Projection) {
                mMapView!!.projection.save(pCanvas, false, true)
                val centerX = pCanvas.getWidth() / 2f
                val centerY = pCanvas.getHeight() / 2f
                pCanvas.drawLine(centerX, centerY, centerX + mOffsetX, centerY + mOffsetY, mPaint)
                mMapView!!.projection.restore(pCanvas, true)
            }
        })

        mMapView!!.setMapCenterOffset(mOffsetX, mOffsetY)
        mMapView!!.post(object : Runnable {
            override fun run() {
                show()
            }
        })
    }

    public override fun onResume() {
        super.onResume()
        alive = true
        //some explanation here.
        //we using a timer task with a delayed start up to move the map around. during CI tests
        //this fragment can crash the app if you navigate away from the fragment before the initial fire
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                runTask()
            }
        }

        t = Timer()
        t!!.schedule(task, 4000, 4000)
    }

    public override fun onPause() {
        super.onPause()
        alive = false
        if (t != null) t!!.cancel()
        t = null
    }


    private fun runTask() {
        if (!alive) return
        if (getActivity() == null) {
            return
        }
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                if (mMapView == null || getActivity() == null) {
                    return
                }
                show()
            }
        })
    }

    private fun show(pIndex: Int = mIndex++) {
        val zoom = 12.5
        val geoPoint = mList.get(pIndex % mList.size)
        mMapView!!.controller!!.animateTo(geoPoint, zoom, null)
    }
}

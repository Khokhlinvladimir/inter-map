package org.osmdroid.bugtestfragments

import android.graphics.Color
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow

/**
 * Created by alex on 8/25/16.
 */
class Bug382Crash : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Bug 382 Crash while scrolling"

    private var polygon: Polygon? = null
    private var polyline: Polyline? = null

    override fun addOverlays() {
        super.addOverlays()
        val geoPoints: MutableList<GeoPoint?> = ArrayList<GeoPoint?>()
        geoPoints.add(GeoPoint(26.0, 113.5))
        geoPoints.add(GeoPoint(26.0, 114.5))
        geoPoints.add(GeoPoint(27.0, 114.5))
        geoPoints.add(GeoPoint(26.0, 115.0))
        geoPoints.add(GeoPoint(26.0, 116.0))
        geoPoints.add(GeoPoint(27.0, 115.0))


        polygon = Polygon(mMapView)
        polygon!!.setPoints(geoPoints.subList(0, 3))
        polygon!!.getFillPaint().setColor(-0x69007e00)
        polygon!!.getOutlinePaint().setColor(Color.RED)
        polygon!!.getOutlinePaint().setStrokeWidth(4f)
        polygon!!.setInfoWindow(BasicInfoWindow(R.layout.bonuspack_bubble, mMapView))
        polygon!!.setTitle("Polygon tapped!")
        mMapView!!.getOverlays()!!.add(polygon)
        mMapView!!.invalidate()

        polyline = Polyline(mMapView)
        polyline!!.setPoints(geoPoints.subList(3, 6))
        polyline!!.getOutlinePaint().setColor(Color.YELLOW)
        polyline!!.getOutlinePaint().setStrokeWidth(8f)
        polyline!!.setInfoWindow(BasicInfoWindow(R.layout.bonuspack_bubble, mMapView))
        polyline!!.setTitle("Polyline tapped!")
        mMapView!!.getOverlays()!!.add(polyline)
        mMapView!!.invalidate()
    }
}

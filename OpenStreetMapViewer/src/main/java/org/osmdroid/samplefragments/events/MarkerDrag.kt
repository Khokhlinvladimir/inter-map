package org.osmdroid.samplefragments.events

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Marker.OnMarkerDragListener
import org.osmdroid.views.overlay.Polyline

/**
 * Based on osmbonuspacks tutorial for dragging markers.
 *
 *
 * Long press to drag the marker, once you yet go, the new location added to a polyline to show it's
 * relative path
 * created on 1/14/2018.
 *
 * @author Alex O'Ree
 */
class MarkerDrag : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Dragging a Marker"

    public override fun addOverlays() {
        super.addOverlays()
        //0. Using the Marker overlay
        val startMarker = Marker(mMapView)
        startMarker.setPosition(GeoPoint(0.0, 0.0))
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.setTitle("Start point")
        startMarker.setDraggable(true)
        startMarker.setOnMarkerDragListener(OnMarkerDragListenerDrawer())
        mMapView!!.getOverlays()!!.add(startMarker)
    }

    //0. Using the Marker and Polyline overlays - advanced options
    internal inner class OnMarkerDragListenerDrawer : OnMarkerDragListener {
        var mTrace: ArrayList<GeoPoint?>
        var mPolyline: Polyline

        init {
            mTrace = ArrayList<GeoPoint?>(100)
            mPolyline = Polyline(mMapView)
            mPolyline.getOutlinePaint().setColor(-0x55ffff01)
            mPolyline.getOutlinePaint().setStrokeWidth(2.0f)
            mPolyline.setGeodesic(true)
            mMapView!!.getOverlays()!!.add(mPolyline)
        }

        override fun onMarkerDrag(marker: Marker?) {
            //mTrace.add(marker.getPosition());
        }

        override fun onMarkerDragEnd(marker: Marker) {
            mTrace.add(marker.getPosition())
            mPolyline.setPoints(mTrace)
            mMapView!!.invalidate()
        }

        override fun onMarkerDragStart(marker: Marker?) {
            //mTrace.add(marker.getPosition());
        }
    }
}

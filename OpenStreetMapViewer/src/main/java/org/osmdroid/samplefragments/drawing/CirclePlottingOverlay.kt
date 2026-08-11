package org.osmdroid.samplefragments.drawing

import android.util.Log
import android.view.MotionEvent
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

/**
 * created on 12/27/2017.
 *
 * @author Alex O'Ree
 */
class CirclePlottingOverlay(var distanceKm: Float) : Overlay() {
    override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
        if (instance!!.isDebugMapView) {
            Log.d(IMapView.LOGTAG, "CirclePlottingOverlay onLongPress")
        }
        val pt = mapView.projection.fromPixels(e.getX().toInt(), e.getY().toInt(), null) as GeoPoint

        /*
         * <b>Note</b></b: when plotting a point off the map, the conversion from
         * screen coordinates to map coordinates will return values that are invalid from a latitude,longitude
         * perspective. Sometimes this is a wanted behavior and sometimes it isn't. We are leaving it up to you,
         * the developer using osmdroid to decide on what is right for your application. See
         * <a href="https://github.com/osmdroid/osmdroid/pull/722">https://github.com/osmdroid/osmdroid/pull/722</a>
         * for more information and the discussion associated with this.
         */

        //just in case the point is off the map, let's fix the coordinates
        if (pt.longitude < -180) pt.setLongitude(pt.longitude + 360)
        if (pt.longitude > 180) pt.setLongitude(pt.longitude - 360)
        //latitude is a bit harder. see https://en.wikipedia.org/wiki/Mercator_projection
        if (pt.latitude > 85.05112877980659) pt.setLatitude(85.05112877980659)
        if (pt.latitude < -85.05112877980659) pt.setLatitude(-85.05112877980659)

        val circle: MutableList<GeoPoint?> = Polygon.pointsAsCircle(pt, distanceKm.toDouble())
        val p = Polygon(mapView)
        p.setPoints(circle)
        p.setTitle("A circle")
        mapView.getOverlayManager().add(p)
        mapView.invalidate()
        return true
    }
}

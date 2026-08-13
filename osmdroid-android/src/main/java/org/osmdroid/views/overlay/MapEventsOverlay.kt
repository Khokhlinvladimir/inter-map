package org.osmdroid.views.overlay

import android.content.Context
import android.view.MotionEvent
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Empty overlay than can be used to detect events on the map,
 * and to throw them to a MapEventsReceiver.
 *
 * @author M.Kergall
 * @see MapEventsReceiver
 */
class MapEventsOverlay(receiver: MapEventsReceiver) : Overlay() {
    private val mReceiver: MapEventsReceiver

    /**
     * Use [.MapEventsOverlay] instead
     */
    @Deprecated("")
    constructor(ctx: Context?, receiver: MapEventsReceiver) : this(receiver)

    /**
     * @param receiver the object that will receive/handle the events.
     * It must implement MapEventsReceiver interface.
     */
    init {
        mReceiver = receiver
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView?): Boolean {
        mapView ?: return false
        val proj = mapView.projection
        val p = proj.fromPixels(e.getX().toInt(), e.getY().toInt()) as GeoPoint?
        return mReceiver.singleTapConfirmedHelper(p)
    }

    override fun onLongPress(e: MotionEvent, mapView: MapView?): Boolean {
        mapView ?: return false
        val proj = mapView.projection
        val p = proj.fromPixels(e.getX().toInt(), e.getY().toInt()) as GeoPoint?
        //throw event to the receiver:
        return mReceiver.longPressHelper(p)
    }
}


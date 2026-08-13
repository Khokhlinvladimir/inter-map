package org.osmdroid.samplefragments.models

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.OverlayItem

class SampleOverlayItem(
    aUid: String?, aTitle: String?, aDescription: String?, aGeoPoint: GeoPoint?,
    aMarker: Drawable?, aHotspotPlace: HotspotPlace?
) : OverlayItem(aUid, aTitle, aDescription, aGeoPoint) {
    init {
        this.setMarker(aMarker)
        this.setMarkerHotspot(aHotspotPlace)
    }

    fun draw(canvas: Canvas?) {
        //
    }
}

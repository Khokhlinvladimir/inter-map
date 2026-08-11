package org.osmdroid.samplefragments.drawing

import android.view.View

/**
 * Demos turning off map repeating
 * Map replication is OFF for this sample (only viewable for numerically lower zoom levels (higher altitude))
 *
 * @since 6.0.0
 * Created by Maradox on 11/26/17.
 */
class SampleDrawPolylineWithoutVerticalWrapping : SampleDrawPolyline(), View.OnClickListener {
    override val sampleTitle: String?
        get() = "Draw a polyline on screen without vertical wrapping"

    public override fun addOverlays() {
        super.addOverlays()
        this.mMapView!!.setHorizontalMapRepetitionEnabled(true)
        this.mMapView!!.setVerticalMapRepetitionEnabled(false)
    }
}

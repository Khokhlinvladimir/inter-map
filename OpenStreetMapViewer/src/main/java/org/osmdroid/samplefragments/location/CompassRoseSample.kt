package org.osmdroid.samplefragments.location

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.overlay.compass.CompassOverlay

/**
 * created on 1/29/2018.
 *
 * @author Alex O'Ree
 */
class CompassRoseSample : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Compass Rose"


    public override fun addOverlays() {
        super.addOverlays()
        val overlay = CompassOverlay(getContext(), mMapView)
        overlay.setPointerMode(false)
        overlay.enableCompass()
        mMapView!!.getOverlayManager().add(overlay)
        mMapView!!.invalidate()
    } //NOTE This sample uses sensors, see base class for lifecycle management
}

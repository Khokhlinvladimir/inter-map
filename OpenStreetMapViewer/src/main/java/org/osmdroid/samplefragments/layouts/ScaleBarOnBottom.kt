package org.osmdroid.samplefragments.layouts

import android.content.Context
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.overlay.ScaleBarOverlay

/**
 * created on 1/8/2017.
 *
 * @author Alex O'Ree
 */
class ScaleBarOnBottom : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Scale Bar on the bottom"

    public override fun addOverlays() {
        super.addOverlays()
        val context: Context? = this.getActivity()
        val dm = context!!.getResources().getDisplayMetrics()

        val scaleBarOverlay = ScaleBarOverlay(mMapView)

        scaleBarOverlay.setCentred(true)

        //api15 and up, 85 is right at the bottom
        //we are also adding 20dp padding for the overlay overlay which is added by the super class
        scaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, dm.heightPixels - (105 * dm.density).toInt())

        scaleBarOverlay.setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.imperial)
        mMapView!!.getOverlayManager().add(scaleBarOverlay)
    }
}

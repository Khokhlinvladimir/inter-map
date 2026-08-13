package org.osmdroid.samplefragments.data

import android.graphics.drawable.BitmapDrawable
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay

/**
 * see https://github.com/osmdroid/osmdroid/issues/477
 *
 *
 * Created by alex on 11/23/2016.
 */
class SampleSimpleLocation : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Simple Location Overlay (marker)"

    public override fun addOverlays() {
        super.addOverlays()
        val drawable = getResources().getDrawable(R.drawable.icon) as BitmapDrawable

        val layer = SimpleLocationOverlay(drawable.getBitmap())
        layer.setLocation(GeoPoint(38.8976763, -77.0365298))
        mMapView!!.getOverlayManager().add(layer)
    }
}

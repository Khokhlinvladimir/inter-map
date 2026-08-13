package org.osmdroid.samplefragments.tilesources

import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment

/**
 * Issue https://github.com/osmdroid/osmdroid/issues/330
 *
 * @since 5.2+
 * Created by alex on 8/14/16.
 */
class SampleCustomLoadingImage : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Custom tile loading image"

    override fun addOverlays() {
        super.addOverlays()
        mMapView!!.getOverlayManager().getTilesOverlay()!!.setLoadingDrawable(getResources().getDrawable(R.drawable.loading))
    }
}

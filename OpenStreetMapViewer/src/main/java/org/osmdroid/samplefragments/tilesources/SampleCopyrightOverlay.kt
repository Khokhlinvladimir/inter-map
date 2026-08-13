package org.osmdroid.samplefragments.tilesources

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.overlay.CopyrightOverlay

/**
 * creates an in your face, ugly, copyright banner
 * created on 1/3/2017.
 *
 * @author Alex O'Ree
 */
class SampleCopyrightOverlay : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Copyright with offsets"

    public override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated")

        mMapView!!.getOverlays()!!.clear()
        val copyrightOverlay = CopyrightOverlay(requireActivity())
        copyrightOverlay.setTextColor(Color.GREEN)
        copyrightOverlay.setTextSize(20)
        copyrightOverlay.setAlignBottom(true)
        copyrightOverlay.setAlignRight(false)
        copyrightOverlay.setOffset(20, 40)

        //with align bottom and left, this should be 20dp from the bottom, 20dp from the left
        mMapView!!.getOverlays()!!.add(copyrightOverlay)
    }
}

package org.osmdroid.samplefragments

import android.os.Bundle
import android.util.Log
import android.view.View
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.OnFirstLayoutListener
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

/**
 * Created by alex on 9/14/16.
 */
class SampleTester : BaseSampleFragment(), OnFirstLayoutListener {
    override val sampleTitle: String
        get() = "Alex's Tester"

    override fun addOverlays() {
        //sorry for the spaghetti code this is to filter out the compass on api 8
        //Note: the compass overlay causes issues on API 8 devices. See https://github.com/osmdroid/osmdroid/issues/218
        mCompassOverlay = CompassOverlay(
            requireContext(), InternalCompassOrientationProvider(requireContext()),
            mMapView
        )
        mCompassOverlay!!.enableCompass()
        mMapView!!.getOverlays()!!.add(this.mCompassOverlay)
    }

    private var mCompassOverlay: CompassOverlay? = null

    override fun onFirstLayout(v: View?, left: Int, top: Int, right: Int, bottom: Int) {
        Log.i("OsmBootUp", "onFirstLayout fired")
        mMapView!!.zoomToBoundingBox(BoundingBox(44.0, -76.0, 43.0, -77.0), true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mMapView!!.addOnFirstLayoutListener(this)
    }

    public override fun onPause() {
        super.onPause()
        //sorry for the spaghetti code this is to filter out the compass on api 8
        //Note: the compass overlay causes issues on API 8 devices. See https://github.com/osmdroid/osmdroid/issues/218
        if (mCompassOverlay != null) {
            this.mCompassOverlay!!.disableCompass()
        }
    }

    public override fun onResume() {
        super.onResume()

        //sorry for the spaghetti code this is to filter out the compass on api 8
        //Note: the compass overlay causes issues on API 8 devices. See https://github.com/osmdroid/osmdroid/issues/218
        if (mCompassOverlay != null) {
            //this call is needed because onPause, the orientation provider is destroyed to prevent context leaks
            this.mCompassOverlay!!.setOrientationProvider(InternalCompassOrientationProvider(requireActivity()))
            this.mCompassOverlay!!.enableCompass()
        }
    }
}

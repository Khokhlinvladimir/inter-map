package org.osmdroid.samplefragments.events

import android.util.Log
import android.view.View
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.OnFirstLayoutListener

/**
 * A quick tutorial on how to listen for when the map is ready to go<br></br>
 * [issue 324](https://github.com/osmdroid/osmdroid/issues/324)
 * Created by alex on 6/4/16.
 */
class SampleMapBootListener : BaseSampleFragment(), OnFirstLayoutListener {
    override val sampleTitle: String
        get() = "Start up events"

    override fun addOverlays() {
        mMapView!!.addOnFirstLayoutListener(this)
    }

    override fun onFirstLayout(v: View?, left: Int, top: Int, right: Int, bottom: Int) {
        Log.i("OsmBootUp", "onFirstLayout fired")
        mMapView!!.zoomToBoundingBox(BoundingBox(44.0, -76.0, 43.0, -77.0), true)
    }
}

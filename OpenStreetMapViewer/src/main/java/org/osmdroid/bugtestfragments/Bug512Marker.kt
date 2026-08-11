package org.osmdroid.bugtestfragments

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * created on 1/7/2017.
 *
 * @author Alex O'Ree
 */
class Bug512Marker : BaseSampleFragment() {
    var marker: Marker? = null

    override val sampleTitle: String
        get() = "Bug 512 Marker infowindow leaks"

    public override fun addOverlays() {
        super.addOverlays()
        marker = Marker(mMapView)
        marker!!.setSnippet("Hello world, bug 512 part 1")
        marker!!.setPosition(GeoPoint(-40.0, -74.0))
        mMapView!!.controller!!.setCenter(marker!!.getPosition())
        mMapView!!.getOverlayManager().add(marker)
    }

    override fun skipOnCiTests(): Boolean {
        return true
    }

    @Throws(Exception::class)
    override fun runTestProcedures() {
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                marker!!.showInfoWindow()
            }
        })
        Thread.sleep(500)
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                marker!!.closeInfoWindow()
                mMapView!!.getOverlayManager().remove(marker)
                marker!!.onDetach(mMapView)

                marker = Marker(mMapView)
                marker!!.setSnippet("Hello world, bug 512 part 2")
                marker!!.setPosition(GeoPoint(-40.0, -74.0))
                mMapView!!.controller!!.setCenter(marker!!.getPosition())
                mMapView!!.getOverlayManager().add(marker)
            }
        })

        Thread.sleep(500)
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                marker!!.showInfoWindow()
            }
        })
        Thread.sleep(500)
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                marker!!.closeInfoWindow()
                mMapView!!.getOverlayManager().remove(marker)
                marker!!.onDetach(mMapView)
            }
        })
    }
}

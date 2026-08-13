package org.osmdroid.bugtestfragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.events.SampleMapEventListener
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * [https://github.com/osmdroid/osmdroid/issues/164](https://github.com/osmdroid/osmdroid/issues/164)
 * Created by alex on 8/28/16.
 */
class Bug164EndlessOnScolls : BaseSampleFragment(), View.OnClickListener {
    override val sampleTitle: String
        get() = "Bug #164 Endless onScroll callsScoll"

    var textViewCurrentLocation: TextView? = null
    var animateTo: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.map_with_locationbox164, container, false)
        mMapView = root.findViewById<MapView?>(R.id.mapview)
        textViewCurrentLocation = root.findViewById<TextView>(R.id.textViewCurrentLocation)
        animateTo = root.findViewById<Button>(R.id.animateTo)
        animateTo!!.setOnClickListener(this)
        Log.d(TAG, "onCreateView")
        return root
    }


    var callsScoll: Int = 0
    var callsZoom: Int = 0

    override fun addOverlays() {
        super.addOverlays()
        //
        mMapView!!.setMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                Log.i(TAG, "onScroll called")
                callsScoll++
                updateInfo()
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                Log.i(TAG, "onZoom called")
                callsZoom++
                updateInfo()
                return true
            }
        })
    }

    private fun updateInfo() {
        val mapCenter = mMapView!!.mapCenter
        textViewCurrentLocation!!.setText(
            (SampleMapEventListener.df.format(mapCenter!!.latitude) + "," +
                    SampleMapEventListener.df.format(mapCenter.longitude)
                    + "," + mMapView!!.zoomLevelDouble + "\nonScroll: " + callsScoll + " onZoom: "
                    + callsZoom)
        )
    }

    override fun onClick(v: View) {
        if (v.getId() == R.id.animateTo) {
            val lat = Math.random() * 180.0 - 90
            val lon = Math.random() * 360 - 180
            mMapView!!.controller!!.animateTo(GeoPoint(lat, lon))
        }
    }
}

package org.osmdroid.samplefragments.events

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.api.IMapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.OnFirstLayoutListener

/**
 * Created by Dad on 10/28/2016.
 */
class ZoomToBoundsOnStartup : BaseSampleFragment(), View.OnClickListener {
    var textViewCurrentLocation: TextView? = null
    var animateTo: Button? = null

    override val sampleTitle: String
        get() = "Zoom to bounds on Start"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.map_with_locationbox164, container, false)

        mMapView = root.findViewById<MapView?>(R.id.mapview)
        mMapView!!.controller!!.setZoom(7)
        animateTo = root.findViewById<Button>(R.id.animateTo)
        animateTo!!.setOnClickListener(this)
        textViewCurrentLocation = root.findViewById<TextView>(R.id.textViewCurrentLocation)
        attach()
        return root

        /*
values from onFirstLayout
18=13
17=13
16=13
15=13
14=13
13=13
12=13
11=13
10=13
7=13
6=13
5=13
4=13
3=14
2=15
1=10
0=10


on a button click
18=13,18
17=13
16=13
15=13
14=13
13=13
12=13
11=13
10=13
7=13
6=13
5=13
4=13
3=14,15
2=15,15
1=10,15
0=10,15
 */
    }

    private fun attach() {
        mMapView!!.addOnFirstLayoutListener(object : OnFirstLayoutListener {
            override fun onFirstLayout(v: View?, left: Int, top: Int, right: Int, bottom: Int) {
            }
        })
    }

    override fun addOverlays() {
        super.addOverlays()
        mMapView!!.setMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onScroll " + event.x + "," + event.y)
                updateInfo()
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onZoom " + event.zoomLevel)
                updateInfo()
                return true
            }
        })
    }

    private fun updateInfo() {
        val mapCenter = mMapView!!.mapCenter
        textViewCurrentLocation!!.setText(
            (SampleMapEventListener.Companion.df.format(mapCenter!!.latitude) + "," +
                    SampleMapEventListener.Companion.df.format(mapCenter.longitude)
                    + "," + mMapView!!.zoomLevelDouble)
        )
    }

    override fun onClick(v: View?) {
        val boundingBox = BoundingBox(41.906802, 12.445436, 41.900073, 12.457852)
        mMapView!!.zoomToBoundingBox(boundingBox, false)
        mMapView!!.zoomToBoundingBox(boundingBox, false)
        mMapView!!.invalidate()

        //Log.d(LOGTAG, "ZoomToBoundingBox calculations: " + maxZoomLatitudeSpan + ","+maxZoomLongitudeSpan + ","+requiredLatitudeZoom + ","+requiredLongitudeZoom );
        //D/OsmDroid: ZoomToBoundingBox calculations: 1.367585271809038E-4,9.655952453613281E-4,13.0,15.0
        //D/OsmDroid: ZoomToBoundingBox calculations: 0.0011179235048756064,9.655952453613281E-4,16.0,15.0
    }
}

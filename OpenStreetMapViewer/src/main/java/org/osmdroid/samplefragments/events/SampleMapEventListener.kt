package org.osmdroid.samplefragments.events

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.api.IMapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import java.text.DecimalFormat

/**
 * used for testing this issue
 * https://github.com/osmdroid/osmdroid/issues/248
 * Created by alex on 2/22/16.
 */
open class SampleMapEventListener : BaseSampleFragment() {
    var textViewCurrentLocation: TextView? = null
    override val sampleTitle: String
        get() = "Map Event Listener"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.map_with_locationbox, container, false)

        mMapView = root.findViewById<MapView?>(R.id.mapview)
        textViewCurrentLocation = root.findViewById<TextView>(R.id.textViewCurrentLocation)
        return root
    }

    override fun addOverlays() {
        super.addOverlays()
        updateInfo()

        mMapView!!.setTileSource(TileSourceFactory.USGS_SAT)
        mMapView!!.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onScroll " + event.x + "," + event.y)
                //Toast.makeText(getActivity(), "onScroll", Toast.LENGTH_SHORT).show();
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
            (df.format(mapCenter!!.latitude) + "," +
                    df.format(mapCenter.longitude)
                    + ",zoom=" + mMapView!!.zoomLevelDouble + "\nBounds: " + mMapView!!.getBoundingBox().toString())
        )
    }

    companion object {
        @JvmField
        val df: DecimalFormat = DecimalFormat("#.000000")
    }
}

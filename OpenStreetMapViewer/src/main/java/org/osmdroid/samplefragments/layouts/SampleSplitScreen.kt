package org.osmdroid.samplefragments.layouts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.osmdroid.R
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.MapBoxTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Uses OSM as the upper map and MapBox (API key required) as the lower map
 * Created by alex on 6/4/16.
 */
class SampleSplitScreen : BaseSampleFragment(), MapListener {
    override val sampleTitle: String
        get() = "Two maps, split screen with Mapbox"

    protected var mMapView2: MapView? = null

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.map_splitscreen, container, false)

        mMapView = root.findViewById<MapView?>(R.id.mapview1)
        mMapView2 = root.findViewById<MapView>(R.id.mapview2)

        return root
    }

    override fun addOverlays() {
        mMapView!!.setTileSource(TileSourceFactory.MAPNIK)
        mMapView!!.controller!!.setZoom(1)
        mMapView!!.controller!!.setCenter(GeoPoint(39.8282, 98.5795))
        mMapView2!!.controller!!.setZoom(1)
        mMapView2!!.controller!!.setCenter(GeoPoint(39.8282, 98.5795))
        mMapView!!.setMapListener(this)
        //hey, check out the other constructors for mapbox, there's a few options to load up your
        //access token and tile set preferences
        mMapView2!!.setTileSource(MapBoxTileSource(requireContext()))
        mMapView2!!.setMapListener(this)

        mMapView2!!.setMultiTouchControls(true)
        mMapView2!!.setTilesScaledToDpi(true)
    }

    var lastEvent: Long = 0

    override fun onScroll(event: ScrollEvent): Boolean {
        if (lastEvent + 40 < System.currentTimeMillis()) {
            lastEvent = System.currentTimeMillis()
            if (event.source === mMapView) {
                mMapView2!!.controller!!.setCenter(mMapView!!.mapCenter)
            } else {
                mMapView!!.controller!!.setCenter(mMapView2!!.mapCenter)
            }
        }

        return true
    }

    override fun onZoom(event: ZoomEvent): Boolean {
        if (lastEvent + 40 < System.currentTimeMillis()) {
            lastEvent = System.currentTimeMillis()
            if (event.source === mMapView) {
                mMapView2!!.controller!!.setZoom(event.zoomLevel)
            } else mMapView!!.controller!!.setZoom(event.zoomLevel)
        }
        return true
    }
}

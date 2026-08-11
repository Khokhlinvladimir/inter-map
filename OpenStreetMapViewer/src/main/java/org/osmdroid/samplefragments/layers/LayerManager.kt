package org.osmdroid.samplefragments.layers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import org.osmdroid.R
import org.osmdroid.api.IMapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Marker.OnMarkerClickListener
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.text.DecimalFormat

/**
 * Views the current layers in a navigation drawer layout
 * created on 2/18/2018.
 *
 * @author Alex O'Ree
 */
class LayerManager : BaseSampleFragment() {
    private var mPlanetTitles: Array<String?>? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerList: ListView? = null

    override val sampleTitle: String
        get() = "Layer Manager"

    var textViewCurrentLocation: TextView? = null
    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.layermanage_drawer, container, false)

        mMapView = root.findViewById<MapView?>(R.id.mapview)
        textViewCurrentLocation = root.findViewById<TextView>(R.id.textViewCurrentLocation)


        mPlanetTitles = arrayOf<String?>("Layer 1", "Layer 2")
        mDrawerLayout = root.findViewById<DrawerLayout?>(R.id.drawer_layout)
        mDrawerList = root.findViewById<ListView>(R.id.left_drawer)
        val adapter = OverlayAdapter(getContext()!!, mMapView!!.getOverlayManager())
        // Set the adapter for the list view
        mDrawerList!!.setAdapter(adapter)
        // Set the list's click listener
        mDrawerList!!.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val overlay = adapter.getItem(position)
                if (overlay is Marker) {
                    overlay.showInfoWindow()
                    mMapView!!.controller!!.animateTo(overlay.getPosition())
                } else if (overlay is Polygon) {
                    overlay.showInfoWindow()
                    mMapView!!.controller!!.animateTo(overlay.getInfoWindowLocation())
                } else if (overlay is Polyline) {
                    overlay.showInfoWindow()
                    mMapView!!.controller!!.animateTo(overlay.getInfoWindowLocation())
                } else {
                    val bounds = overlay!!.getBounds()
                    mMapView!!.controller!!.animateTo(GeoPoint(bounds.getCenterLatitude(), bounds.getCenterLongitude()))

                    //mMapView.getController().zoomToSpan(bounds.getLatitudeSpan(), bounds.getLongitudeSpan());
                }
                //TODO center map on location
            }
        })
        mDrawerList!!.setOnLongClickListener(object : OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                //TODO prompt for confirmation, then remove it from the overlay manager
                return false
            }
        })


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


        //add some simple markers, lines and polygons just to have something to populate the list
        var startPoint = GeoPoint(38.8977, -77.0365) //white house
        var startMarker = Marker(mMapView)
        startMarker.setPosition(startPoint)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.setIcon(getResources().getDrawable(R.drawable.icon))
        startMarker.setTitle("White House")
        startMarker.setSnippet("The White House is the official residence and principal workplace of the President of the United States.")
        startMarker.setSubDescription("1600 Pennsylvania Ave NW, Washington, DC 20500")
        mMapView!!.getOverlays()!!.add(startMarker)

        startPoint = GeoPoint(38.8719, -77.0563)
        startMarker = Marker(mMapView)
        startMarker.setPosition(startPoint)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.setIcon(getResources().getDrawable(R.drawable.icon))
        startMarker.setTitle("Pentagon")
        startMarker.setSnippet("The Pentagon.")
        startMarker.setSubDescription("The Pentagon is the headquarters of the United States Department of Defense.")
        startMarker.setOnMarkerClickListener(object : OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker, mapView: MapView?): Boolean {
                marker.showInfoWindow()
                return true
            }
        })
        mMapView!!.getOverlays()!!.add(startMarker)


        startPoint = GeoPoint(38.8895, -77.0353)
        startMarker = Marker(mMapView)
        startMarker.setPosition(startPoint)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.setIcon(getResources().getDrawable(R.drawable.icon))
        startMarker.setTitle("Washington Monument")
        startMarker.setSnippet("Washington Monument.")
        startMarker.setSubDescription("Washington Monument.")
        startMarker.setOnMarkerClickListener(object : OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker, mapView: MapView?): Boolean {
                Toast.makeText(getContext(), marker.getTitle() + " was clicked", Toast.LENGTH_LONG).show()
                marker.showInfoWindow()
                return true
            }
        })
        //startMarker.setInfoWindow(new MarkerInfoWindow());
        mMapView!!.getOverlays()!!.add(startMarker)
        val mNorthPolyline = Polyline()
        val mSouthPolyline = Polyline()
        val mWestPolyline = Polyline()
        val mEastPolyline = Polyline()


        val list = ArrayList<GeoPoint?>()
        val sCentralParkBoundingBox = BoundingBox(
            40.796788,
            -73.949232, 40.768094, -73.981762
        )
        list.add(GeoPoint(sCentralParkBoundingBox.getActualNorth(), -85.0))
        list.add(GeoPoint(sCentralParkBoundingBox.getActualNorth(), -65.0))
        mNorthPolyline.setPoints(list)
        mMapView!!.getOverlays()!!.add(mNorthPolyline)

        list.clear()
        list.add(GeoPoint(sCentralParkBoundingBox.getActualSouth(), -85.0))
        list.add(GeoPoint(sCentralParkBoundingBox.getActualSouth(), -65.0))
        mSouthPolyline.setPoints(list)
        mMapView!!.getOverlays()!!.add(mSouthPolyline)

        list.clear()
        list.add(GeoPoint(45.0, sCentralParkBoundingBox.getLonWest()))
        list.add(GeoPoint(35.0, sCentralParkBoundingBox.getLonWest()))
        mWestPolyline.setPoints(list)
        mMapView!!.getOverlays()!!.add(mWestPolyline)

        list.clear()
        list.add(GeoPoint(45.0, sCentralParkBoundingBox.getLonEast()))
        list.add(GeoPoint(35.0, sCentralParkBoundingBox.getLonEast()))
        mEastPolyline.setPoints(list)
        mMapView!!.getOverlays()!!.add(mEastPolyline)

        mMapView!!.invalidate()
        Toast.makeText(this.mMapView!!.getContext(), "Swipe from the right", Toast.LENGTH_LONG).show()
    }

    private fun updateInfo() {
        val mapCenter = mMapView!!.mapCenter
        textViewCurrentLocation!!.setText(
            (df.format(mapCenter!!.latitude) + "," +
                    df.format(mapCenter.longitude)
                    + ",zoom=" + mMapView!!.zoomLevelDouble)
        )
    }

    companion object {
        val df: DecimalFormat = DecimalFormat("#.000000")
    }
}

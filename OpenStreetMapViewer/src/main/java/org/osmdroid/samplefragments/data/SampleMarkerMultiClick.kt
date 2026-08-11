package org.osmdroid.samplefragments.data

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import org.osmdroid.R
import org.osmdroid.api.IGeoPoint
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.data.SampleItemizedOverlayMultiClick.Companion.data
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Marker.OnMarkerClickListener

/**
 * @author Fabrice Fontaine
 * Sample on how to handle a click on overlapping [Marker]s
 * @since 6.0.3
 */
class SampleMarkerMultiClick : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    private val mClicked: MutableList<Marker> = ArrayList<Marker>()

    override fun addOverlays() {
        super.addOverlays()

        mMapView!!.getOverlays()!!.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (mClicked.size == 0) {
                    return false
                }
                if (mClicked.size == 1) {
                    message(mClicked.get(0))
                    mClicked.clear()
                    return true
                }
                val titles = arrayOfNulls<String>(mClicked.size)
                val items = arrayOfNulls<Marker>(titles.size)
                var i = 0
                for (item in mClicked) {
                    titles[i] = item.getTitle()
                    items[i] = item
                    i++
                }
                AlertDialog.Builder(getActivity()!!)
                    .setItems(titles, object : DialogInterface.OnClickListener {
                        override fun onClick(dialogInterface: DialogInterface?, i: Int) {
                            message(items[i]!!)
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show()
                mClicked.clear()
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }))

        val datas = data
        val geoPoints: MutableList<IGeoPoint> = ArrayList()
        val drawable = getResources().getDrawable(R.drawable.icon)
        for (data in datas) {
            geoPoints.add(requireNotNull(data.geoPoint))
            val marker: Marker = MyMarker(mMapView!!)
            marker.position = GeoPoint(requireNotNull(data.geoPoint))
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = drawable
            marker.setTitle(data.title)
            marker.setSnippet(data.snippet)
            marker.setOnMarkerClickListener(object : OnMarkerClickListener {
                override fun onMarkerClick(marker: Marker?, mapView: MapView?): Boolean {
                    mClicked.add(marker!!)
                    return false
                }
            })
            mMapView!!.getOverlays()!!.add(marker)
        }

        val box = BoundingBox.fromGeoPoints(geoPoints)
        mMapView!!.post(object : Runnable {
            override fun run() {
                mMapView!!.zoomToBoundingBox(box, false, 50)
            }
        })
    }

    private fun message(pMarker: Marker) {
        (pMarker as MyMarker).onMarkerClickDefault(pMarker, mMapView!!)
    }

    private class MyMarker(mapView: MapView) : Marker(mapView) {
        public override fun onMarkerClickDefault(marker: Marker, mapView: MapView): Boolean { // made public
            return super.onMarkerClickDefault(marker, mapView)
        }
    }

    companion object {
        private const val SAMPLE_TITLE: String = "Overlapping Markers' click"
    }
}

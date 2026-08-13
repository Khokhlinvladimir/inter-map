package org.osmdroid.samplefragments.data

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import org.osmdroid.R
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.models.MyMapItem
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * An example using osmbonuspacks polyline class for a simple box on around centra park, nyc
 *
 * @author Marc Kurtz
 */
class SampleOsmPath : BaseSampleFragment(), MapListener {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    private val sCentralParkBoundingBox: BoundingBox

    init {
        sCentralParkBoundingBox = BoundingBox(40.796788, -73.949232, 40.768094, -73.981762)
    }

    public override fun onActivityCreated(savedInstanceState: Bundle?) {
        mMapView!!.controller!!.setZoom(13.0)
        mMapView!!.controller!!.setCenter(sCentralParkBoundingBox.centerWithDateLine)

        super.onActivityCreated(savedInstanceState)
    }

    override fun addOverlays() {
        super.addOverlays()

        //we override this to force zoom to 22, even though mapnik dooesn't do that deep
        val mapnik: OnlineTileSourceBase = XYTileSource(
            "Mapnik",
            0, 22, 256, ".png", arrayOf<String>(
                "https://a.tile.openstreetmap.org/",
                "https://b.tile.openstreetmap.org/",
                "https://c.tile.openstreetmap.org/"
            )
        )
        mMapView!!.getTileProvider()!!.setTileSource(mapnik)


        var line = Polyline(mMapView!!)
        line.setTitle("Central Park, NYC")
        line.setSubDescription(Polyline::class.java.getCanonicalName())
        line.getOutlinePaint().setStrokeWidth(20f)
        var pts: MutableList<GeoPoint> = ArrayList()

        //here, we create a polygon, note that you need 5 points in order to make a closed polygon (rectangle)
        pts.add(GeoPoint(40.796788, -73.949232))
        pts.add(GeoPoint(40.796788, -73.981762))
        pts.add(GeoPoint(40.768094, -73.981762))
        pts.add(GeoPoint(40.768094, -73.949232))
        pts.add(GeoPoint(40.796788, -73.949232))
        line.setPoints(pts)
        line.setGeodesic(true)
        line.setInfoWindow(BasicInfoWindow(R.layout.bonuspack_bubble, mMapView!!))
        //Note, the info window will not show if you set the onclick listener
        //line can also attach click listeners to the line
        /*
		line.setOnClickListener(new Polyline.OnClickListener() {
			@Override
			public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
				Toast.makeText(context, "Hello world!", Toast.LENGTH_LONG).show();
				return false;
			}
		});*/
        mMapView!!.getOverlayManager().add(line)
        mMapView!!.setMaxZoomLevel(22.0)


        val marker = Marker(mMapView!!)
        marker.isDraggable = false
        marker.setTitle("Central Park")
        marker.position =
            GeoPoint(
                ((40.796788 - 40.768094) / 2) + 40.768094,
                ((-73.949232 - -73.981762) / 2) + -73.981762
            )
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = getResources().getDrawable(R.drawable.sfgpuci)
        marker.setTitle("Start point")
        marker.isDraggable = true
        mMapView!!.getOverlays()!!.add(marker)


        //here, we create a polygon using polygon class, note that you need 4 points in order to make a rectangle
        val polygon = Polygon(mMapView!!)
        polygon.setTitle("This is a polygon")
        polygon.setSubDescription(Polygon::class.java.getCanonicalName())
        polygon.getFillPaint()!!.setColor(Color.RED)
        polygon.setVisible(true)
        polygon.getOutlinePaint().setColor(Color.BLACK)
        polygon.setInfoWindow(BasicInfoWindow(R.layout.bonuspack_bubble, mMapView!!))


        pts = ArrayList()
        pts.add(GeoPoint(40.886788, -73.959232))
        pts.add(GeoPoint(40.886788, -73.971762))
        pts.add(GeoPoint(40.878094, -73.971762))
        pts.add(GeoPoint(40.878094, -73.959232))
        polygon.setPoints(pts)
        mMapView!!.getOverlays()!!.add(polygon)


        val m = Marker(mMapView!!)
        m.position = GeoPoint(51.7875, 6.135278)
        m.image = getResources().getDrawable(R.drawable.icon)
        line = Polyline(mMapView!!)
        line.setTitle("TEST")
        line.setSubDescription(Polyline::class.java.getCanonicalName())
        line.getOutlinePaint().setStrokeWidth(20f)
        pts = ArrayList()

        //here, we create a polygon, note that you need 5 points in order to make a closed polygon (rectangle)
        pts.add(GeoPoint(51.7875, 6.135278))
        pts.add(GeoPoint(51.7875, 6.135288))
        pts.add(GeoPoint(51.7874, 6.135288))
        pts.add(GeoPoint(51.7874, 6.135288))
        pts.add(GeoPoint(51.7875, 6.135278))
        line.setPoints(pts)
        line.setGeodesic(true)
        line.setInfoWindow(BasicInfoWindow(R.layout.bonuspack_bubble, mMapView!!))

        mMapView!!.getOverlayManager().add(m)
        mMapView!!.getOverlayManager().add(line)


        val list: MutableList<MyMapItem?> = ArrayList<MyMapItem?>()
        list.add(MyMapItem("title", "description", GeoPoint(51.7875, 6.135278)))
        val layer = ItemizedIconOverlay<MyMapItem?>(list, getResources().getDrawable(R.drawable.shgpuci), object : OnItemGestureListener<MyMapItem?> {
            override fun onItemSingleTapUp(index: Int, item: MyMapItem?): Boolean {
                return false
            }

            override fun onItemLongPress(index: Int, item: MyMapItem?): Boolean {
                return false
            }
        }, getActivity())

        mMapView!!.getOverlayManager().add(layer)
        mMapView!!.addMapListener(this)
    }

    override fun onScroll(event: ScrollEvent): Boolean {
        return false
    }

    override fun onZoom(event: ZoomEvent): Boolean {
        val act: Activity? = getActivity()
        if (act != null) {
            getActivity()!!.runOnUiThread(object : Runnable {
                override fun run() {
                    try {
                        Log.i("Zoomer", "zoom event triggered " + event.zoomLevel)
                        //Toast.makeText(getActivity(), "Zoom is " + event.getZoomLevel(), Toast.LENGTH_SHORT).show();
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            })
        }
        return true
    }

    public override fun skipOnCiTests(): Boolean {
        return true
    }

    public override fun runTestProcedures() {
        val geoPoint = GeoPoint(40.886788, -73.959232)
        var steps = 0
        while (mMapView!!.zoomLevelDouble < mMapView!!.maxZoomLevel && steps < MAX_TEST_ZOOM_STEPS) {
            val nextZoom = min(mMapView!!.maxZoomLevel, mMapView!!.zoomLevelDouble + 1.0)
            runOnUiThreadAndWait {
                mMapView!!.controller!!.animateTo(geoPoint)
                mMapView!!.controller!!.setZoom(nextZoom)
            }
            steps++
        }
        check(mMapView!!.zoomLevelDouble >= mMapView!!.maxZoomLevel) {
            "Map did not reach its maximum zoom after $steps steps"
        }

        val geoPoint2 = GeoPoint(40.796788, -73.949232)
        runOnUiThreadAndWait {
            mMapView!!.controller!!.animateTo(geoPoint2)
        }
    }

    private fun runOnUiThreadAndWait(action: () -> Unit) {
        val latch = CountDownLatch(1)
        requireActivity().runOnUiThread {
            try {
                action()
            } finally {
                latch.countDown()
            }
        }
        check(latch.await(TEST_UI_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "Map UI operation timed out" }
    }

    companion object {
        private const val SAMPLE_TITLE: String = "OsmPath drawing"
        private const val MAX_TEST_ZOOM_STEPS = 32
        private const val TEST_UI_TIMEOUT_SECONDS = 5L
    }
}

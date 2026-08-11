package org.osmdroid.samplefragments.events

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.overlay.Polygon

/**
 * Created by alex on 10/4/16.
 */
class SampleZoomToBounding : BaseSampleFragment(), View.OnClickListener {
    private val tileSystem = getTileSystem()

    private var polygon: Polygon? = null

    /**
     * @since 6.1.0
     * south, north
     */
    private val mSampleLatitudes = doubleArrayOf(0.0, 85.0, -85.0, 0.0)

    /**
     * @since 6.1.0
     */
    private var mSampleLatitudeIndex = 0

    /**
     * @since 6.1.0
     * west, east
     */
    private val mSampleLongitudes = doubleArrayOf(0.0, 10.0, 0.0, 10.0)

    /**
     * @since 6.1.0
     */
    private var mSampleLongitudeIndex = 0

    override val sampleTitle: String
        get() = "Zoom to Bounding Box"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        mMapView = MapView(getActivity()!!)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        polygon = Polygon(mMapView!!)
        val btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache.setOnClickListener(this)
        btnCache.setText("Zoom to bounds")

        polygon!!.getOutlinePaint().setColor(Color.parseColor("#990000FF"))
        polygon!!.getOutlinePaint().setStrokeWidth(2f)
        polygon!!.getFillPaint()!!.setColor(Color.parseColor("#330000FF"))
        mMapView!!.getOverlays()!!.add(polygon)

        return root
    }


    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.btnCache -> {
                var ok = false
                while (!ok) {
                    val south = getRandomLatitude(tileSystem.minLatitude)
                    val north = getRandomLatitude(south)
                    val west = this.randomLongitude
                    var east = this.randomLongitude
                    val boundingBox = BoundingBox(north, east, south, west)
                    val zoom = tileSystem.getBoundingBoxZoom(boundingBox, mMapView!!.getWidth() - 2 * border, mMapView!!.getHeight() - 2 * border)
                    ok = zoom >= mMapView!!.getMinZoomLevel() && zoom <= mMapView!!.maxZoomLevel
                    if (ok) {
                        val text = "with a border of " + border + " the computed zoom is " + zoom + " for box " + boundingBox
                        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show()
                        val points: MutableList<GeoPoint> = ArrayList()
                        if (west > east) {
                            east += 360.0
                        }
                        addPoints(points, north, west, north, east)
                        addPoints(points, north, east, south, east)
                        addPoints(points, south, east, south, west)
                        addPoints(points, south, west, north, west)
                        polygon!!.setPoints(points)
                        mMapView!!.invalidate()
                        mMapView!!.zoomToBoundingBox(boundingBox, true, border)
                    }
                }
            }
        }
    }

    /**
     * Add a succession of GeoPoint's, separated by an increment,
     * taken from the segment between two GeoPoint's
     *
     * @since 6.0.0
     */
    private fun addPoints(
        pPoints: MutableList<GeoPoint>,
        pBeginLat: Double, pBeginLon: Double,
        pEndLat: Double, pEndLon: Double
    ) {
        val increment = 10.0 // in degrees
        pPoints.add(GeoPoint(pBeginLat, pBeginLon))
        var lat = pBeginLat
        var lon = pBeginLon
        val incLat = if (pBeginLat == pEndLat) 0.0 else if (pBeginLat < pEndLat) increment else -increment
        val incLon = if (pBeginLon == pEndLon) 0.0 else if (pBeginLon < pEndLon) increment else -increment
        while (true) {
            if (incLat != 0.0) {
                lat += incLat
                if (incLat < 0) {
                    if (lat < pEndLat) {
                        break
                    }
                } else {
                    if (lat > pEndLat) {
                        break
                    }
                }
            }
            if (incLon != 0.0) {
                lon += incLon
                if (incLon < 0) {
                    if (lon < pEndLon) {
                        break
                    }
                } else {
                    if (lon > pEndLon) {
                        break
                    }
                }
            }
            pPoints.add(GeoPoint(lat, lon))
        }
        pPoints.add(GeoPoint(pEndLat, pEndLon))
    }

    private val randomLongitude: Double
        get() {
            if (mSampleLongitudeIndex < mSampleLongitudes.size) {
                return mSampleLongitudes[mSampleLongitudeIndex++]
            }
            return tileSystem.getRandomLongitude(Math.random())
        }

    private fun getRandomLatitude(pMinLatitude: Double): Double {
        if (mSampleLatitudeIndex < mSampleLatitudes.size) {
            return mSampleLatitudes[mSampleLatitudeIndex++]
        }
        return tileSystem.getRandomLatitude(Math.random(), pMinLatitude)
    }

    companion object {
        private const val border = 10
    }
}

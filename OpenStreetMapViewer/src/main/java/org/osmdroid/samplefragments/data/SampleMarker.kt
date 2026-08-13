package org.osmdroid.samplefragments.data

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.widget.Toast
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.ScaleDiskOverlay

/**
 * An example on using osmbonuspack's Marker class by following the tutorial at
 * https://github.com/MKergall/osmbonuspack/wiki/Tutorial_0
 * https://github.com/MKergall/osmbonuspack/wiki/Tutorial_1
 *
 * created on 12/29/2016.
 *
 * @author Alex O'Ree
 */
class SampleMarker : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Marker"

    override fun addOverlays() {
        super.addOverlays()

        val whiteHouse = GeoPoint(38.8977, -77.0365)
        val pentagon = GeoPoint(38.8719, -77.0563)
        val washington = GeoPoint(38.8895, -77.0353)
        val displayMetrics = requireContext().resources.displayMetrics

        val scaleDiskOverlayWhiteHouse = ScaleDiskOverlay(
            requireContext(), whiteHouse, 2000, GeoConstants.UnitOfMeasure.Foot
        )
        val circlePaint = Paint().apply {
            color = Color.rgb(128, 128, 128)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        scaleDiskOverlayWhiteHouse.setCirclePaint2(circlePaint)
        val diskPaint = Paint().apply {
            color = Color.argb(128, 128, 128, 128)
            style = Paint.Style.FILL_AND_STROKE
        }
        scaleDiskOverlayWhiteHouse.setCirclePaint1(diskPaint)
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 10 * displayMetrics.density
        }
        scaleDiskOverlayWhiteHouse.setTextPaint(textPaint)
        scaleDiskOverlayWhiteHouse.setLabelOffsetBottom((-2 * displayMetrics.density).toInt())
        scaleDiskOverlayWhiteHouse.setLabelOffsetTop((2 * displayMetrics.density).toInt())
        scaleDiskOverlayWhiteHouse.setLabelOffsetLeft((2 * displayMetrics.density).toInt())
        scaleDiskOverlayWhiteHouse.setLabelOffsetRight((-2 * displayMetrics.density).toInt())
        scaleDiskOverlayWhiteHouse.setDisplaySizeMin(100)
        scaleDiskOverlayWhiteHouse.setDisplaySizeMax(800)
        mMapView!!.getOverlays()!!.add(scaleDiskOverlayWhiteHouse)

        val scaleDiskOverlayPentagon = ScaleDiskOverlay(
            requireContext(), pentagon, 1, GeoConstants.UnitOfMeasure.StatuteMile
        )
        scaleDiskOverlayPentagon.setCirclePaint1(Paint().apply {
            color = Color.argb(32, 255, 0, 0)
            style = Paint.Style.FILL
        })
        scaleDiskOverlayPentagon.setTextPaint(Paint().apply {
            isAntiAlias = true
            color = Color.RED
            textSize = 20 * displayMetrics.density
        })
        scaleDiskOverlayPentagon.setLabelOffsetTop((2 * displayMetrics.density).toInt())
        scaleDiskOverlayPentagon.setDisplaySizeMin(100)
        scaleDiskOverlayPentagon.setDisplaySizeMax(800)
        mMapView!!.getOverlays()!!.add(scaleDiskOverlayPentagon)

        val scaleDiskOverlayWashington = ScaleDiskOverlay(
            requireContext(), washington, 2000, GeoConstants.UnitOfMeasure.Foot
        )
        scaleDiskOverlayWashington.setCirclePaint2(Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 2f
        })
        scaleDiskOverlayWashington.setDisplaySizeMin(100)
        scaleDiskOverlayWashington.setDisplaySizeMax(800)
        mMapView!!.getOverlays()!!.add(scaleDiskOverlayWashington)
        mMapView!!.getOverlays()!!.add(ScaleBarOverlay(mMapView!!))

        val points = ArrayList<GeoPoint>()
        val drawable: Drawable = resources.getDrawable(R.drawable.marker_default)

        var startPoint = GeoPoint(whiteHouse)
        points.add(startPoint)
        var startMarker = Marker(mMapView!!).apply {
            position = startPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = drawable
            setTitle("White House")
            setSnippet("The White House is the official residence and principal workplace of the President of the United States.")
            setSubDescription("1600 Pennsylvania Ave NW, Washington, DC 20500")
        }
        mMapView!!.getOverlays()!!.add(startMarker)

        startPoint = GeoPoint(pentagon)
        points.add(startPoint)
        startMarker = Marker(mMapView!!).apply {
            position = startPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = drawable
            setTitle("Pentagon")
            setSnippet("The Pentagon.")
            setSubDescription("The Pentagon is the headquarters of the United States Department of Defense.")
            setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                override fun onMarkerClick(marker: Marker?, mapView: org.osmdroid.views.MapView?): Boolean {
                    marker!!.showInfoWindow()
                    return true
                }
            })
        }
        mMapView!!.getOverlays()!!.add(startMarker)

        startPoint = GeoPoint(washington)
        points.add(startPoint)
        startMarker = Marker(mMapView!!).apply {
            position = startPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = drawable
            setTitle("Washington Monument")
            setSnippet("Washington Monument.")
            setSubDescription("Washington Monument.")
            rotation = 45f
            setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                override fun onMarkerClick(marker: Marker?, mapView: org.osmdroid.views.MapView?): Boolean {
                    Toast.makeText(requireContext(), "${marker!!.getTitle()} was clicked", Toast.LENGTH_LONG).show()
                    marker.showInfoWindow()
                    return true
                }
            })
        }
        mMapView!!.getOverlays()!!.add(startMarker)

        val boundingBox = BoundingBox.fromGeoPoints(points)
        mMapView!!.post {
            mMapView!!.zoomToBoundingBox(boundingBox, false, drawable.intrinsicWidth)
        }
    }
}

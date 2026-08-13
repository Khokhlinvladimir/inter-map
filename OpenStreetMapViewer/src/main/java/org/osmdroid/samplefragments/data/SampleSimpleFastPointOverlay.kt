package org.osmdroid.samplefragments.data

import android.graphics.Color
import android.graphics.Paint
import android.widget.Toast
import org.osmdroid.api.IGeoPoint
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay.PointAdapter
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import org.osmdroid.views.overlay.simplefastpoint.StyledLabelledGeoPoint
import kotlin.math.floor

/**
 * Example of SimpleFastPointOverlay
 * Created by Miguel Porto on 12-11-2016.
 */
class SampleSimpleFastPointOverlay : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Simple Fast Point Overlay with 60k points"

    override fun addOverlays() {
        super.addOverlays()
        // **********************************************
        // Create 30k labelled points sharing same style
        // **********************************************
        // in most cases, there will be no problems of displaying >100k points, feel free to try
        val points: MutableList<IGeoPoint?> = ArrayList<IGeoPoint?>()
        for (i in 0..29999) {
            points.add(
                LabelledGeoPoint(
                    37 + Math.random() * 5, -8 + Math.random() * 5,
                    "Point #" + i
                )
            )
        }

        // wrap them in a theme
        val pointTheme = SimplePointTheme(points)

        // create label style
        val textStyle = Paint()
        textStyle.setStyle(Paint.Style.FILL)
        textStyle.setColor(Color.parseColor("#0000ff"))
        textStyle.setTextAlign(Paint.Align.CENTER)
        textStyle.setTextSize(24f)

        // set some visual options for the overlay
        // we use here MAXIMUM_OPTIMIZATION algorithm, which works well with >100k points
        var opt = SimpleFastPointOverlayOptions.defaultStyle
            .setSymbol(SimpleFastPointOverlayOptions.Shape.SQUARE)
            .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
            .setRadius(7f).setIsClickable(true).setCellSize(12).setTextStyle(textStyle)
            .setMinZoomShowLabels(10)

        // create the overlay with the theme
        val sfpo = SimpleFastPointOverlay(pointTheme, opt)

        // onClick callback
        sfpo.setOnClickListener(object : SimpleFastPointOverlay.OnClickListener {
            override fun onClick(points: PointAdapter?, point: Int?) {
                Toast.makeText(
                    mMapView!!.getContext(),
                    "You clicked " + (points!!.get(point!!) as LabelledGeoPoint).label,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // add overlay
        mMapView!!.getOverlays()!!.add(sfpo)

        // *****************************************************
        // Now add another layer with points individually styled
        // *****************************************************
        // create 30k labelled points
        val individualStyledPoints: MutableList<IGeoPoint?> = ArrayList<IGeoPoint?>()
        var indPointStyle: Paint?
        var indTextStyle: Paint?

        for (i in 0..29999) {
            // create random colored style for each point
            indPointStyle = Paint()
            indPointStyle.setStyle(Paint.Style.FILL)
            indPointStyle.setColor(
                Color.rgb(
                    floor(Math.random() * 255).toInt(),
                    floor(Math.random() * 255).toInt(), floor(Math.random() * 255).toInt()
                )
            )

            // create style with random color and text size for each point label
            indTextStyle = Paint()
            indTextStyle.setTextSize((10 + Math.random() * 30).toInt().toFloat())
            indTextStyle.setTextAlign(Paint.Align.CENTER)
            indTextStyle.setColor(
                Color.rgb(
                    floor(Math.random() * 255).toInt(),
                    floor(Math.random() * 255).toInt(), floor(Math.random() * 255).toInt()
                )
            )
            indTextStyle.setStyle(Paint.Style.FILL)

            individualStyledPoints.add(
                StyledLabelledGeoPoint(
                    37 + Math.random() * 5, -3 + Math.random() * 5,
                    "Point #" + i, indPointStyle, indTextStyle
                )
            )
        }

        // wrap point list in a theme
        val individualStyledPointTheme = SimplePointTheme(individualStyledPoints)

        // set some visual options for the theme
        opt = SimpleFastPointOverlayOptions.defaultStyle
            .setSymbol(SimpleFastPointOverlayOptions.Shape.SQUARE)
            .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
            .setRadius(7f).setCellSize(12).setMinZoomShowLabels(10)

        // create the overlay with the theme
        val sfpo1 = SimpleFastPointOverlay(individualStyledPointTheme, opt)

        // add overlay
        mMapView!!.getOverlays()!!.add(sfpo1)

        // zoom to both themes' bounding box
        mMapView!!.postDelayed(object : Runnable {
            override fun run() {
                if (mMapView != null && mMapView!!.controller != null && mMapView!!.getIntrinsicScreenRect(null)
                        .height() > 0
                ) mMapView!!.zoomToBoundingBox(requireNotNull(sfpo.boundingBox).concat(requireNotNull(sfpo1.boundingBox)), false)
            }
        }, 500L)
    }
}


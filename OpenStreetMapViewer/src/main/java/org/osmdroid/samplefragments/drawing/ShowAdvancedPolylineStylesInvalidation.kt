package org.osmdroid.samplefragments.drawing

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingForScalarContainer
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingVariationHue
import org.osmdroid.views.overlay.advancedpolyline.MonochromaticPaintList
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList

/**
 * Simple example to show scalar mapping invalidation.
 *
 * @author Matthias Dittmer
 */
class ShowAdvancedPolylineStylesInvalidation : BaseSampleFragment(), View.OnClickListener {
    /*
         * Example data
         */
    private var mLineExtended = false
    private var textInformation: TextView? = null
    private var btnProceed: Button? = null
    private var mPolyline: Polyline? = null
    private var mMapping: ColorMappingVariationHue? = null
    private var mContainer: ColorMappingForScalarContainer? = null
    private val mInformation = "Scalar range from %d to %d\n" +
            "for hue ranging from %d to %d.\n" +
            "Showing speed from red (slow) to green (fast)."
    private val sProceed = "Extend Polyline"
    private val sReset = "Reset Polyline"
    private val paintBorder = Paint()
    private val paintMapping = Paint()

    // Simple wrapper class to group a point and scalar together
    // No getters and setters
    internal class PointWithScalar(var mPoint: GeoPoint?, var mScalar: Float)

    // list holding the initial and extended data
    private var mInitialData: ArrayList<PointWithScalar>? = null
    private var mExtendedData: ArrayList<PointWithScalar>? = null

    override val sampleTitle: String
        get() = "Show advanced polyline (with invalidation)"

    public override fun addOverlays() {
        super.addOverlays()
        initialSetupForLine()
    }

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.layout_advanced_polyline_invalidate, null)

        // setup UI references
        mMapView = v.findViewById<MapView?>(R.id.mapview)
        textInformation = v.findViewById<TextView>(R.id.textInformation)
        btnProceed = v.findViewById<Button>(R.id.btnProceed)
        btnProceed!!.setOnClickListener(this)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // create border paint
        paintBorder.setColor(Color.BLACK)
        paintBorder.setAntiAlias(true)
        paintBorder.setStrokeWidth(25f)
        paintBorder.setStyle(Paint.Style.STROKE)
        paintBorder.setStrokeJoin(Paint.Join.ROUND)
        paintBorder.setStrokeCap(Paint.Cap.ROUND)
        paintBorder.setAntiAlias(true)

        // create mapping paint
        paintMapping.setAntiAlias(true)
        paintMapping.setStrokeWidth(20f)
        paintMapping.setStyle(Paint.Style.FILL_AND_STROKE)
        paintMapping.setStrokeJoin(Paint.Join.ROUND)
        paintMapping.setStrokeCap(Paint.Cap.ROUND)
        paintMapping.setAntiAlias(true)

        // setup initial data
        mInitialData = ArrayList<PointWithScalar>()
        mInitialData!!.add(PointWithScalar(GeoPoint(37.0, -11.0), 10f))
        mInitialData!!.add(PointWithScalar(GeoPoint(37.0, -11.0), 0.0f))
        mInitialData!!.add(PointWithScalar(GeoPoint(37.5, -11.5), 20.0f))
        mInitialData!!.add(PointWithScalar(GeoPoint(38.0, -11.0), 10.0f))
        mInitialData!!.add(PointWithScalar(GeoPoint(38.5, -11.5), 30.0f))
        mInitialData!!.add(PointWithScalar(GeoPoint(39.0, -11.0), 50.0f))
        mInitialData!!.add(PointWithScalar(GeoPoint(39.5, -11.5), 25.0f))

        // setup extended data
        // please note: the last scalar is not used, N points use N - 1 scalars
        mExtendedData = ArrayList<PointWithScalar>()
        mExtendedData!!.add(PointWithScalar(GeoPoint(40.0, -11.0), 80.0f))
        mExtendedData!!.add(PointWithScalar(GeoPoint(40.5, -11.5), 60f))
        mExtendedData!!.add(PointWithScalar(GeoPoint(41.0, -11.0), 100.0f))
        mExtendedData!!.add(PointWithScalar(GeoPoint(41.5, -11.5), 100.0f))

        // center to line once here
        centerToLine()
    }

    /*
     * Creates initial line.
     */
    private fun initialSetupForLine() {
        // remove previous data

        if (mPolyline != null) {
            // remove polyline
            mMapView!!.getOverlayManager().remove(mPolyline)
            mPolyline = null
            mMapping = null
            mContainer = null
        }

        // create polyline
        mPolyline = Polyline(mMapView, false, false)

        // setup border
        mPolyline!!.getOutlinePaintLists().add(MonochromaticPaintList(paintBorder))

        // setup mapping objects
        mMapping = ColorMappingVariationHue(MIN_SCALAR.toFloat(), MAX_SCALAR.toFloat(), MIN_HUE.toFloat(), MAX_HUE.toFloat(), SAT, LUM)
        mContainer = ColorMappingForScalarContainer(mMapping)

        // add initial data to polyline
        addDataToPolyline(mInitialData!!)

        // setup the mapping
        mPolyline!!.getOutlinePaintLists().add(PolychromaticPaintList(paintMapping, mMapping, true))

        // update UI
        mMapView!!.getOverlayManager().add(mPolyline)
        // force a redraw (normally triggered when map is moved for example)
        mMapView!!.invalidate()
        textInformation!!.setText(String.format(mInformation, MIN_SCALAR, MAX_SCALAR, MIN_HUE, MAX_HUE))
        btnProceed!!.setText(sProceed)
    }

    /*
     * Extends the line and invalidates the mapping with a new scalar range.
     */
    private fun extendAndInvalidateLine() {
        // extend data of polyline

        addDataToPolyline(mExtendedData!!)

        // update mapping with scalar end updated from 50 to 100
        // new "top speed" of "100"
        mMapping!!.init(MIN_SCALAR.toFloat(), MAX_SCALAR_EXTENDED.toFloat(), MIN_HUE.toFloat(), MAX_HUE.toFloat())

        // call refresh to update line
        mContainer!!.refresh()

        // force a redraw (normally triggered when map is moved for example)
        mMapView!!.invalidate()

        // update UI
        textInformation!!.setText(String.format(mInformation, MIN_SCALAR, MAX_SCALAR_EXTENDED, MIN_HUE, MAX_HUE))
        btnProceed!!.setText(sReset)
    }

    // add geopoint to polyline and scalar to container from provided list
    private fun addDataToPolyline(pData: ArrayList<PointWithScalar>) {
        for (element in pData) {
            mPolyline!!.addPoint(element.mPoint)
            mContainer!!.add(element.mScalar)
        }
    }

    // centers roughly to line
    private fun centerToLine() {
        mMapView!!.post(object : Runnable {
            override fun run() {
                mMapView!!.controller!!.setCenter(GeoPoint(38.5, -11.5))
                mMapView!!.controller!!.zoomTo(6.0)
            }
        })
    }

    override fun onClick(view: View) {
        // simple toggle logic
        if (view.getId() == R.id.btnProceed) {
            if (mLineExtended) {
                initialSetupForLine()
                mLineExtended = false
            } else {
                extendAndInvalidateLine()
                mLineExtended = true
            }
        }
    }

    companion object {
        // min / max values used in the example
        // scalar meaning is "speed" in this example with no unit
        const val MIN_SCALAR: Int = 0
        const val MAX_SCALAR: Int = 50
        const val MAX_SCALAR_EXTENDED: Int = 100

        // hue range from red for "slow" to green for "fast"
        const val MIN_HUE: Int = 0 // red
        const val MAX_HUE: Int = 120 // green
        const val SAT: Float = 1.0f
        const val LUM: Float = 0.5f
    }
}

package org.osmdroid.samplefragments.drawing

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.ColorMapping
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingCycle
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingForScalar
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingPlain
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingRanges
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingVariationHue
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingVariationLuminance
import org.osmdroid.views.overlay.advancedpolyline.ColorMappingVariationSaturation
import org.osmdroid.views.overlay.advancedpolyline.MonochromaticPaintList
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList
import org.osmdroid.views.overlay.infowindow.InfoWindow
import java.io.InputStreamReader
import java.io.Reader
import java.util.SortedMap
import java.util.TreeMap

/**
 * Showing all modes of advanced polyline styles with example data.
 *
 * @author Matthias Dittmer
 */
class ShowAdvancedPolylineStyles : BaseSampleFragment(), View.OnClickListener {
    /**
     * List with all examples.
     */
    private var mListExamples: ArrayList<AdvancedPolylineExample> = ArrayList<AdvancedPolylineExample>()

    /**
     * JSON object holding the complete example data.
     */
    private val JSON_EXAMPLE_DATA = "example_data_advanced_polyline.json"
    var mData: JSONObject? = null

    override val sampleTitle: String
        get() = "Show advanced polyline styles"

    public override fun addOverlays() {
        super.addOverlays()
        addSamplePolylines()
        recenter(0)
    }

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        mMapView = MapView(getActivity()!!)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        val btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache.setOnClickListener(this)
        btnCache.setText("Next example")

        return root
    }

    override fun onClick(view: View?) {
        recenter(1)
    }

    fun addSamplePolylines() {
        // load JSON data

        loadJSONDataFromAssets()

        // setup all examples
        setupExamples()

        // add all examples from array
        for (example in mListExamples) {
            mMapView!!.getOverlayManager().add(example.polyline)
            // show info window so line is easy to spot for user
            //example.getPolyline().showInfoWindow();
        }
    }

    private fun recenter(pIndex: Int) {
        mListExamples.get(mIndex).polyline.closeInfoWindow()
        mIndex += pIndex
        mIndex = mIndex % mListExamples.size
        getmMapView()!!.post(object : Runnable {
            override fun run() {
                val example = mListExamples.get(mIndex)
                getmMapView()!!.zoomToBoundingBox(example.mBoundingBox, false)
                example.polyline.showInfoWindow()
            }
        })
    }

    private var mIndex = 0

    /**
     * Class to hold on example.
     */
    internal inner class AdvancedPolylineExample(
        title: String?, description: String?,
        mapping: ColorMapping?, gradient: Boolean,
        borderColor: Int?, pClosePath: Boolean,
        points: MutableList<GeoPoint>, pScalars: MutableList<Float?>?
    ) {
        var polyline: Polyline
            private set
        val mBoundingBox: BoundingBox?

        init {
            // setup polyline
            this.polyline = Polyline(mMapView!!, false, pClosePath)

            if (borderColor != null) {
                val paint = Paint()
                paint.setColor(borderColor)
                paint.setAntiAlias(true)
                paint.setStrokeWidth(25f)
                paint.setStyle(Paint.Style.STROKE)
                paint.setStrokeJoin(Paint.Join.ROUND)
                paint.setStrokeCap(Paint.Cap.ROUND)
                paint.setAntiAlias(true)
                polyline.getOutlinePaintLists().add(MonochromaticPaintList(paint))
            }

            // add points and scalar
            polyline.setPoints(points)
            if (mapping is ColorMappingForScalar) {
                val mappingForScalar = mapping
                for (scalar in pScalars.orEmpty()) {
                    mappingForScalar.add(scalar!!)
                }
            }

            val paint = Paint()
            paint.setAntiAlias(true)
            paint.setStrokeWidth(20f)
            paint.setStyle(Paint.Style.FILL_AND_STROKE)
            paint.setStrokeJoin(Paint.Join.ROUND)
            paint.setStrokeCap(Paint.Cap.ROUND)
            paint.setAntiAlias(true)
            polyline.getOutlinePaintLists().add(PolychromaticPaintList(paint, requireNotNull(mapping), gradient))

            // set a bounding box from points, plus 1.2f scaled
            mBoundingBox = BoundingBox.fromGeoPoints(points).increaseByScale(1.2f)

            // add infowindow
            val infoWindow: InfoWindowExample
            infoWindow = InfoWindowExample(R.layout.bonuspack_bubble, mMapView!!)
            infoWindow.setText(title, description)
            polyline.setInfoWindow(infoWindow)
        }
    }

    /**
     * Infowindow
     */
    internal inner class InfoWindowExample(layoutResId: Int, mapView: MapView) : InfoWindow(layoutResId, mapView) {
        init {
            mView!!.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View?) {
                    close()
                }
            })
        }

        fun setText(title: String?, description: String?) {
            (getView()!!.findViewById<View?>(R.id.bubble_title) as TextView).setText(title)
            (getView()!!.findViewById<View?>(R.id.bubble_description) as TextView).setText(description)
        }

        override fun onOpen(item: Any?) {
        }

        override fun onClose() {
        }
    }

    private fun setupExamples() {
        // Plain example
        mListExamples.add(
            AdvancedPolylineExample(
                "Sailing", "Plain colored polyline showing a sailing track from Sicily to Sardinia.",
                ColorMappingPlain(Color.WHITE),
                false, Color.BLACK, false,
                getPoints("sailing"), null
            )
        )

        // Cycle example
        mListExamples.add(
            AdvancedPolylineExample(
                "Coast", "Cycle polyline showing border of Italy coast line.\n\nColor cycle: GREEN, WHITE, RED.",
                ColorMappingCycle(intArrayOf(Color.GREEN, Color.WHITE, Color.RED)),
                true, Color.BLACK, false,
                getPoints("border_coast_italy"), null
            )
        )

        // Ranges example
        val mColorRanges: SortedMap<Float?, Int?> = TreeMap<Float?, Int?>()
        mColorRanges.put(5.0f, Color.RED)
        mColorRanges.put(7.5f, Color.YELLOW)
        mColorRanges.put(10.0f, Color.GREEN)
        mListExamples.add(
            AdvancedPolylineExample(
                "Tram",
                "Ranges polyline with border showing a tram ride between airport and main train station.\n\nBorders: 5 m/s RED, 7.5 m/s YELLOW, 10.0 m/s GREEN.",
                ColorMappingRanges(mColorRanges, true),
                false,
                Color.BLACK,
                false,
                getPoints("tram"),
                getScalars("tram")
            )
        )

        // Hue example
        mListExamples.add(
            AdvancedPolylineExample(
                "Flight",
                "Hue variation polyline for speed of plane from Paris to Philadelphia.\n\nHue from 0.0f to 120.0f for speed range 0 km/h to 1000 km/h.",
                ColorMappingVariationHue(0.0f, 1000.0f, 0.0f, 120.0f, 1.0f, 0.5f),
                false,
                Color.BLACK,
                false,
                getPoints("flight_paris_phil"),
                getScalars("flight_paris_phil")
            )
        )

        // Saturation example
        mListExamples.add(
            AdvancedPolylineExample(
                "Flight",
                "Saturation variation polyline for speed of plane from Frankfurt to Bangkok.\n\nSaturation from 0.0f to 1.0f for speed range 0 km/h to 1100 km/h.",
                ColorMappingVariationSaturation(0.0f, 1100.0f, 0.0f, 1.0f, 160.0f, 0.5f),
                false,
                Color.BLACK,
                false,
                getPoints("flight_fra_bkk"),
                getScalars("flight_fra_bkk")
            )
        )

        // Luminance example
        mListExamples.add(
            AdvancedPolylineExample(
                "Hiking",
                "Luminance variation polyline for height of hiking track in Nepal Himalayas.\n\nLuminance from 0.0f to 1.0f for height range 1800 m to 6000 m.",
                ColorMappingVariationLuminance(1800.0f, 6000.0f, 0.0f, 1.0f, 0.0f, 0.0f),
                false,
                Color.BLACK,
                false,
                getPoints("nepal_himalayas"),
                getScalars("nepal_himalayas")
            )
        )

        // Loop example
        val hexagon: MutableList<GeoPoint> = ArrayList()
        hexagon.add(GeoPoint(51.038333, 2.377500)) // Dunkerque
        hexagon.add(GeoPoint(48.573333, 7.752200)) // Strasbourg
        hexagon.add(GeoPoint(43.695833, 7.271389)) // Nice
        hexagon.add(GeoPoint(42.698611, 2.895556)) // Perpignan
        hexagon.add(GeoPoint(43.481617, -1.556111)) // Biarritz
        hexagon.add(GeoPoint(48.390833, -4.468889)) // Brest
        val colorMappingCycle = ColorMappingCycle(
            intArrayOf( // rainbow
                Color.RED,
                Color.rgb(0xFF, 0x7f, 0),  // orange
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.rgb(0x7F, 0, 0xFF) // violet
            )
        )
        colorMappingCycle.setGeoPointNumber(hexagon.size)
        mListExamples.add(
            AdvancedPolylineExample(
                "Loop", "Test about closed Polylines",
                colorMappingCycle,
                true, Color.BLACK, true,
                hexagon, null
            )
        )
    }

    private fun loadJSONDataFromAssets() {
        try {
            val inputStream = getContext()!!.getAssets().open(JSON_EXAMPLE_DATA)
            val bufferSize = 1024
            val buffer = CharArray(bufferSize)
            val out = StringBuilder()
            val `in`: Reader = InputStreamReader(inputStream, "UTF-8")
            while (true) {
                val rsz = `in`.read(buffer, 0, buffer.size)
                if (rsz < 0) break
                out.append(buffer, 0, rsz)
            }

            // parse into JSON object
            mData = JSONObject(out.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPoints(identifier: String): ArrayList<GeoPoint> {
        val points = ArrayList<GeoPoint>()
        try {
            val example = mData!!.get(identifier) as JSONObject
            val array = example.getJSONArray("geopoints")

            var i = 0
            while (i < array.length()) {
                val lat = array.getDouble(i)
                val lon = array.getDouble(i + 1)
                points.add(GeoPoint(lat, lon))
                i += 2
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return points
    }

    private fun getScalars(identifier: String): ArrayList<Float?> {
        val scalars = ArrayList<Float?>()
        try {
            val example = mData!!.get(identifier) as JSONObject
            val array = example.getJSONArray("scalars")

            for (i in 0 until array.length()) {
                val scalar = array.getDouble(i)
                scalars.add(scalar.toFloat())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return scalars
    }
}


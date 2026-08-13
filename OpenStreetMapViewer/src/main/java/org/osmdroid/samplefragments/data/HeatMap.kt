package org.osmdroid.samplefragments.data

import android.graphics.Color
import android.util.DisplayMetrics
import android.util.Log
import org.osmdroid.api.IGeoPoint
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import kotlin.math.abs

/**
 * EXPERIMENTAL!!
 * 
 * [https://github.com/osmdroid/osmdroid/issues/499](https://github.com/osmdroid/osmdroid/issues/499)
 * 
 * 
 * Demonstrates a way to generate heatmaps using osmdroid and a collection of data points.
 * There's a lot of room for improvement but this class demonstrates two things
 * 
 *  * How to load data asynchronously when the map moves/zooms
 *  * How to generate a basic heat map
 * 
 * 
 * 
 * There's probably a many options to implement this. This example basically chops up the screen
 * into cells, generates some random data, then iterates all of the data and increments up a
 * counter based on the cell that it was rendered into. Finally the cells are converted into square
 * polygons with a fill color based on the counter.
 * 
 * 
 * It's assumed that all required data is available on device for this example.
 * 
 * 
 * For future readers: other approaches
 * 
 *  * if a server/network connection is available, it would be better to have the server
 * generate a kml/kmz for the heat map, then use osmbonuspack to do the parsing. this will be much
 * better at handling higher volumes of data
 *  * use a server that generates slippy map tiles representing the overlay, then add a secondary [org.osmdroid.views.overlay.TilesOverlay] with that source.
 *  * locally (on device) generate an image for the slippy map tiles representing the data, then add a secondary [org.osmdroid.views.overlay.TilesOverlay] with that source.
 *  * make a custom [Overlay] class that has some custom onDraw logical to paint the image.
 * 
 * All of these other (and better) approaches really need some kind of geospatial index mechanism, such
 * as [this](https://github.com/davidmoten/rtree), only modified with some kind of running
 * estimate algorithm.
 * 
 * 
 * created on 1/1/2017.
 * 
 * @author Alex O'Ree
 * @since 5.6.3
 */
class HeatMap : BaseSampleFragment(), MapListener, Runnable {
    override val sampleTitle: String
        get() = "Heatmap with Async loading"

    var TAG: String = "heatmap"
    var dm: DisplayMetrics? = null

    // async loading stuff
    var renderJobActive: Boolean = false
    var running: Boolean = true
    var lastMovement: Long = 0
    var needsDataRefresh: Boolean = true


    // end async loading stuff
    /**
     * the size of the cell in density independent pixels
     * a higher value = smoother image but higher processing and rendering times
     */
    var cellSizeInDp: Int = 20


    //colors and alpha settings
    var alpha: String = "#55"
    var red: String = "FF0000"
    var orange: String = "FFA500"
    var yellow: String = "FFFF00"

    //a pointer to the last render overlay, so that we can remove/replace it with the new one
    var heatmapOverlay: FolderOverlay? = null


    public override fun addOverlays() {
        super.addOverlays()
        dm = getResources().getDisplayMetrics()
        mMapView!!.controller!!.setCenter(GeoPoint(38.8977, -77.0365))
        mMapView!!.controller!!.setZoom(14)
        mMapView!!.setMapListener(this)
    }

    public override fun onPause() {
        super.onPause()
        running = false
    }

    public override fun onResume() {
        super.onResume()
        running = true
        val t = Thread(this)
        t.start()
    }

    /**
     * this generates the heatmap off of the main thread, loads the data, makes the overlay, then
     * adds it to the map
     */
    private fun generateMap() {
        if (getActivity() == null)  //java.lang.IllegalStateException: Fragment HeatMap{44f341d0} not attached to Activity
            return
        if (renderJobActive) return
        renderJobActive = true

        val mapView = mMapView ?: run {
            renderJobActive = false
            return
        }


        val densityDpi = (dm!!.density * cellSizeInDp).toInt()

        //10 dpi sized cells
        val iGeoPoint = mapView.projection.fromPixels(0, 0)
        val iGeoPoint2 = mapView.projection.fromPixels(densityDpi, densityDpi)
        //delta is the size of our cell in lat,lon
        //since this is zoom dependent, rerun the calculations on zoom changes
        val xCellSizeLongitude = abs(iGeoPoint!!.longitude - iGeoPoint2!!.longitude)
        val yCellSizeLatitude = abs(iGeoPoint.latitude - iGeoPoint2.latitude)

        val view = mapView.getBoundingBox()
        //a set of a GeoPoints representing what we want a heat map of.
        val pts = loadPoints(view!!)

        //the highest value in our collection of stuff
        var maxHeat = 0

        //a temp container of all grid cells and their hit count (which turns into a color on render)

        //the lower the cell size the more cells and items in the map.
        val heatmap: MutableMap<BoundingBox?, Int?> = HashMap<BoundingBox?, Int?>()

        //create the grid
        Log.i(TAG, "heatmap builder " + yCellSizeLatitude + " " + xCellSizeLongitude)
        Log.i(TAG, "heatmap builder " + view)

        //populate the cells
        var lat = view.latNorth
        while (lat >= view.latSouth) {
            var lon = view.lonEast
            while (lon >= view.lonWest) {
                //Log.i(TAG,"heatmap builder " + lat + "," + lon);
                heatmap.put(BoundingBox(lat, lon, lat - yCellSizeLatitude, lon - xCellSizeLongitude), 0)
                lon = lon - xCellSizeLongitude
            }
            lat = lat - yCellSizeLatitude
        }


        Log.i(TAG, "generating the heatmap")
        var now = System.currentTimeMillis()

        //generate the map, put the items in each cell
        for (i in pts.indices) {
            //get the box for this pt's coordinates
            val x = increment(pts.get(i)!!, heatmap)
            if (x > maxHeat) maxHeat = x
        }
        Log.i(TAG, "generating the heatmap, done " + (System.currentTimeMillis() - now))

        //figure out the color scheme
        //if you need a more logirthmic scale, this is the place to do it.
        //cells with a 0 value are blank
        //cells 1 to 1/3 of the max value are yellow
        //cells from 1/3 to 2/3 are organge
        //cells 2/3 or higher are red
        val redthreshold = maxHeat * 2 / 3 //upper 1/3
        val orangethreshold = maxHeat * 1 / 3 //middle 1/3

        //render the map
        Log.i(TAG, "rendering")
        now = System.currentTimeMillis()
        //each bounding box if the hit count > 0 create a polygon with the bounding box coordinates with the right fill color
        val group = FolderOverlay()
        val iterator = heatmap.entries.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.value!! > 0) {
                group.add(createPolygon(next.key!!, next.value!!, redthreshold, orangethreshold))
            }
        }
        Log.i(TAG, "render done , done " + (System.currentTimeMillis() - now))
        if (getActivity() == null || mMapView !== mapView) {
            renderJobActive = false
            return
        }
        mapView.post(object : Runnable {
            override fun run() {
                if (mMapView !== mapView) {
                    renderJobActive = false
                    return
                }
                if (heatmapOverlay != null) mapView.getOverlayManager().remove(heatmapOverlay)
                mapView.getOverlayManager().add(group)
                heatmapOverlay = group

                mapView.invalidate()
                renderJobActive = false
            }
        })
    }


    /**
     * generates a bunch of random data
     * 
     * @param view
     * @return
     */
    private fun loadPoints(view: BoundingBox): MutableList<IGeoPoint?> {
        val pts: MutableList<IGeoPoint?> = ArrayList<IGeoPoint?>()

        for (i in 0..9999) {
            pts.add(
                GeoPoint(
                    (Math.random() * view.latitudeSpan) + view.latSouth,
                    (Math.random() * view.longitudeSpan) + view.lonWest
                )
            )
        }
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))
        pts.add(GeoPoint(0.0, 0.0))


        pts.add(GeoPoint(1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(1.1 * cellSizeInDp, 1.1 * cellSizeInDp))

        pts.add(GeoPoint(-1.1 * cellSizeInDp, -1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, -1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, -1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, -1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, -1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, -1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, -1.1 * cellSizeInDp))

        pts.add(GeoPoint(-1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, 1.1 * cellSizeInDp))
        pts.add(GeoPoint(-1.1 * cellSizeInDp, 1.1 * cellSizeInDp))


        pts.add(GeoPoint(1.1 * cellSizeInDp, -1.1 * cellSizeInDp))
        pts.add(GeoPoint(1.1 * cellSizeInDp, -1.1 * cellSizeInDp))
        pts.add(GeoPoint(1.1 * cellSizeInDp, -1.1 * cellSizeInDp))

        return pts
    }

    /**
     * converts the bounding box into a color filled polygon
     * 
     * @param key
     * @param value
     * @param redthreshold
     * @param orangethreshold
     * @return
     */
    private fun createPolygon(key: BoundingBox, value: Int, redthreshold: Int, orangethreshold: Int): Overlay {
        // Heat-map cells have no info windows. Keeping them independent of the MapView also
        // prevents a background render from touching a repository that has already detached.
        val polygon = Polygon()
        if (value < orangethreshold) polygon.getFillPaint()!!.setColor(Color.parseColor(alpha + yellow))
        else if (value < redthreshold) polygon.getFillPaint()!!.setColor(Color.parseColor(alpha + orange))
        else if (value >= redthreshold) polygon.getFillPaint()!!.setColor(Color.parseColor(alpha + red))
        else {
            //no polygon
        }
        polygon.getOutlinePaint().setColor(polygon.getFillPaint()!!.getColor())

        //if you set this to something like 20f and have a low alpha setting,
        // you'll end with a gaussian blur like effect
        polygon.getOutlinePaint().setStrokeWidth(0f)
        val pts: MutableList<GeoPoint> = ArrayList()
        pts.add(GeoPoint(key.latNorth, key.lonWest))
        pts.add(GeoPoint(key.latNorth, key.lonEast))
        pts.add(GeoPoint(key.latSouth, key.lonEast))
        pts.add(GeoPoint(key.latSouth, key.lonWest))
        polygon.setPoints(pts)
        return polygon
    }

    /**
     * For each data point, find the corresponding cell, then increment the count. This is the
     * most inefficient portion of this example.
     * 
     * 
     * room for improvement: replace with some kind of geospatial indexing mechanism
     * 
     * @param iGeoPoint
     * @param heatmap
     * @return
     */
    private fun increment(iGeoPoint: IGeoPoint, heatmap: MutableMap<BoundingBox?, Int?>): Int {
        val iterator = heatmap.entries.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.key!!.contains(iGeoPoint)) {
                val newval = next.value!! + 1
                heatmap.put(next.key, newval)
                return newval
            }
        }
        return 0
    }

    /**
     * handles the map movement rendering portions, prevents more than one render at a time,
     * waits for the user to stop moving the map before triggering the render
     */
    override fun onScroll(event: ScrollEvent): Boolean {
        lastMovement = System.currentTimeMillis()
        needsDataRefresh = true
        return false
    }

    /**
     * handles the map movement rendering portions, prevents more than one render at a time,
     * waits for the user to stop moving the map before triggering the render
     */
    override fun onZoom(event: ZoomEvent): Boolean {
        lastMovement = System.currentTimeMillis()
        needsDataRefresh = true
        return false
    }

    /**
     * handles the map movement rendering portions, prevents more than one render at a time,
     * waits for the user to stop moving the map before triggering the render
     */
    override fun run() {
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
        }
        //TODO replace me with a timer task
        while (running) {
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (needsDataRefresh) {
                if (System.currentTimeMillis() - lastMovement > 500) {
                    generateMap()
                    needsDataRefresh = false
                }
            }
        }
    }
}

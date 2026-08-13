package org.osmdroid.views.overlay.gridlines

import android.content.Context
import android.graphics.Color
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Latitude/Longitude gridline overlay
 *
 *
 * It's not perfect and has issues with osmdroid's global wrap around (where north pole turns into the south pole).
 * There's probably room for more optimizations too, pull requests are welcome.
 *
 * @see LatLonGridlineOverlay2
 *
 * @since 5.2+
 * Created by alex on 12/15/15.
 */
@Deprecated("see {@link LatLonGridlineOverlay2}")
object LatLonGridlineOverlay {
    val df: DecimalFormat = DecimalFormat("#.#####")
    var lineColor: Int = Color.BLACK
    var fontColor: Int = Color.WHITE
    var fontSizeDp: Short = 24
    var backgroundColor: Int = Color.BLACK
    var lineWidth: Float = 1f

    //extra debugging options
    var DEBUG: Boolean = false
    var DEBUG2: Boolean = false

    //used to adjust the number of grid lines displayed on screen
    private const val multiplier = 1f

    private fun applyMarkerAttributes(m: Marker) {
        m.textLabelBackgroundColor = backgroundColor
        m.textLabelFontSize = fontSizeDp.toInt()
        m.textLabelForegroundColor = fontColor
    }

    fun getLatLonGrid(ctx: Context?, mapView: MapView): FolderOverlay {
        val box = mapView.getBoundingBox()
        val zoom = mapView.zoomLevel

        if (DEBUG) {
            println("######### getLatLonGrid ")
        }
        val gridlines = FolderOverlay()
        if (zoom < 2) {
            /*  commented out for performance reasons
          the calculations due to wrap around screw things up because the bounds is more than 1 globe.
            for (int i = -90; i <= 90; i = i + 45) {
                Polyline p = new Polyline(ctx);
                p.setColor(mLineColor);
                p.setWidth(lineWidth);
                List<GeoPoint> pts = new ArrayList<GeoPoint>();

                GeoPoint x = new GeoPoint((double) i, 180);
                pts.add(x);
                x = new GeoPoint((double) i, 0);
                pts.add(x);
                x = new GeoPoint((double) i, -180);
                pts.add(x);

                p.setPoints(pts);
                gridlines.add(p);
            }

            //vertical lines

            for (int i = -180; i < 180; i = i + 45) {
                Polyline p = new Polyline(ctx);
                p.setColor(mLineColor);
                p.setWidth(lineWidth);
                List<GeoPoint> pts = new ArrayList<GeoPoint>();

                GeoPoint x = new GeoPoint((double) 90, (double) i);
                pts.add(x);
                x = new GeoPoint((double) -90, (double) i);
                pts.add(x);
                p.setPoints(pts);
                gridlines.add(p);
            }*/
        } else {
            val north = box!!.latNorth
            val south = box.latSouth
            val east = box.lonEast
            val west = box.lonWest

            val north_south_delta = 0.0

            if (north < south) {
                //we're vertically wrapping, abort.
                return gridlines
            }
            if (DEBUG) {
                println("N " + north + " S " + south + ", " + north_south_delta)
            }

            var dateLineVisible = false
            if (east < 0 && west > 0) {
                //we're at the date line
                dateLineVisible = true
            }

            if (DEBUG) {
                println("delta " + north_south_delta)
            }

            //drop a line every this many degrees
            val incrementor = getIncrementor(zoom)


            //this should be starting south at the nearest logical value, 90,45, 15, 10, 5, 1, 0.5, 0.25, 0.125, based on the incrementer,
            //that way doesn't look like the lines are dancing everywhere
            //FIXME also draw 2x as wide as the screen, to support rotation?
            val startend = getStartEndPointsNS(north, south, zoom)
            val sn_start_point = startend[0]
            val sn_stop_point = startend[1]


            run {
                var i = sn_start_point
                while (i <= sn_stop_point) {
                    val p = Polyline()
                    p.getOutlinePaint().setStrokeWidth(LatLonGridlineOverlay.lineWidth)
                    p.getOutlinePaint().setColor(LatLonGridlineOverlay.lineColor)
                    val pts: MutableList<GeoPoint> = ArrayList()


                    var gx = GeoPoint(i, east)
                    pts.add(gx)
                    gx = GeoPoint(i, west)
                    pts.add(gx)
                    if (LatLonGridlineOverlay.DEBUG) {
                        println("drawing NS " + i + "," + east + " to " + i + "," + west + ", zoom " + zoom)
                    }

                    p.setPoints(pts)

                    gridlines.add(p)


                    val m = Marker(mapView)
                    LatLonGridlineOverlay.applyMarkerAttributes(m)
                    val title = LatLonGridlineOverlay.df.format(i) + (if (i > 0) "N" else "S")
                    m.setTitle(title)
                    m.setTextIcon(title)
                    m.position = GeoPoint(i, west + incrementor)
                    gridlines.add(m)
                    i = i + incrementor
                }
            }

            val ew = getStartEndPointsWE(west, east, zoom)
            val we_startpoint = ew[1]
            val ws_stoppoint = ew[0]


            var i = we_startpoint
            while (i <= ws_stoppoint) {
                val p = Polyline()
                p.getOutlinePaint().setStrokeWidth(lineWidth)
                p.getOutlinePaint().setColor(lineColor)
                val pts: MutableList<GeoPoint> = ArrayList()
                var gx = GeoPoint(north, i)
                pts.add(gx)
                gx = GeoPoint(south, i)
                pts.add(gx)
                p.setPoints(pts)

                if (DEBUG) {
                    System.err.println("drawing EW " + south + "," + i + " to " + north + "," + i + ", zoom " + zoom)
                }
                gridlines.add(p)


                val m = Marker(mapView)
                applyMarkerAttributes(m)
                m.rotation = -90f
                val title = df.format(i) + (if (i > 0) "E" else "W")
                m.setTitle(title)
                m.setTextIcon(title)
                m.position = GeoPoint(south + incrementor, i)
                gridlines.add(m)
                i = i + incrementor
            }
            if (dateLineVisible) {
                if (DEBUG) println("DATELINE zoom " + zoom + " " + we_startpoint + " " + ws_stoppoint)

                //special case to ensure that vertical lines are visible when the date line is visible.
                //in this case western point is very positive and eastern part is very negative
                run {
                    var i = we_startpoint
                    while (i <= 180) {
                        val p = Polyline()
                        p.getOutlinePaint().setStrokeWidth(LatLonGridlineOverlay.lineWidth)
                        p.getOutlinePaint().setColor(LatLonGridlineOverlay.lineColor)
                        val pts: MutableList<GeoPoint> = ArrayList()
                        var gx = GeoPoint(north, i)
                        pts.add(gx)
                        gx = GeoPoint(south, i)
                        pts.add(gx)
                        p.setPoints(pts)

                        if (LatLonGridlineOverlay.DEBUG2) {
                            println("DATELINE drawing NS" + south + "," + i + " to " + north + "," + i + ", zoom " + zoom)
                        }

                        gridlines.add(p)

                        i = i + incrementor
                    }
                }
                run {
                    var i = -180.0
                    while (i <= ws_stoppoint) {
                        val p = Polyline()
                        p.getOutlinePaint().setStrokeWidth(LatLonGridlineOverlay.lineWidth)
                        p.getOutlinePaint().setColor(LatLonGridlineOverlay.lineColor)
                        val pts: MutableList<GeoPoint> = ArrayList()
                        var gx = GeoPoint(north, i)
                        pts.add(gx)
                        gx = GeoPoint(south, i)
                        pts.add(gx)
                        p.setPoints(pts)

                        if (LatLonGridlineOverlay.DEBUG2) {
                            println("DATELINE drawing EW" + south + "," + i + " to " + north + "," + i + ", zoom " + zoom)
                        }

                        gridlines.add(p)

                        val m = Marker(mapView)
                        LatLonGridlineOverlay.applyMarkerAttributes(m)
                        m.rotation = -90f
                        val title = LatLonGridlineOverlay.df.format(i) + (if (i > 0) "E" else "W")
                        m.setTitle(title)
                        m.setTextIcon(title)
                        m.position = GeoPoint(south + incrementor, i)
                        gridlines.add(m)
                        i = i + incrementor
                    }
                }


                var i = we_startpoint
                while (i < 180) {
                    val m = Marker(mapView)

                    applyMarkerAttributes(m)
                    m.rotation = -90f
                    val title = df.format(i) + (if (i > 0) "E" else "W")
                    m.setTitle(title)
                    m.setTextIcon(title)
                    m.position = GeoPoint(south + incrementor, i)
                    gridlines.add(m)
                    i = i + incrementor
                }
            }
        }
        return gridlines
    }

    /**
     * gets the start and end points for a latitude line
     *
     * @param north
     * @param south
     * @param zoom
     * @return
     */
    private fun getStartEndPointsNS(north: Double, south: Double, zoom: Int): DoubleArray {
        //brute force when zoom is less than 10
        if (zoom < 10) {
            var sn_start_point = floor(south)
            val incrementor = getIncrementor(zoom)


            var x = -90.0
            while (x < sn_start_point) x = x + incrementor
            sn_start_point = x

            var sn_stop_point = ceil(north)
            x = 90.0
            while (x > sn_stop_point) x = x - incrementor
            sn_stop_point = x

            if (sn_stop_point > 90) {
                sn_stop_point = 90.0
            }
            if (sn_start_point < -90) {
                sn_start_point = -90.0
            }
            return doubleArrayOf(sn_start_point, sn_stop_point)
        } else {
            //hmm start at origin, add inc until we go too far, then back off, go to the next zoom level
            var sn_start_point = -90.0
            if (south > 0) {
                sn_start_point = 0.0
            }
            var sn_stop_point = 90.0
            if (north < 0) {
                sn_stop_point = 0.0
            }

            for (xx in 2..zoom) {
                val inc = getIncrementor(xx)
                while (sn_start_point < south - inc) {
                    sn_start_point += inc
                    if (DEBUG) {
                        println("south " + sn_start_point)
                    }
                }

                while (sn_stop_point > north + inc) {
                    sn_stop_point -= inc
                    if (DEBUG) {
                        println("north " + sn_stop_point)
                    }
                }
            }

            return doubleArrayOf(sn_start_point, sn_stop_point)
        }
    }


    /**
     * gets the start and stop point for a longitude line
     *
     * @param west
     * @param east
     * @param zoom
     * @return
     */
    private fun getStartEndPointsWE(west: Double, east: Double, zoom: Int): DoubleArray {
        val incrementor = getIncrementor(zoom)
        //brute force when zoom is less than 10
        if (zoom < 10) {
            var we_startpoint = floor(west)
            var x = 180.0
            while (x > we_startpoint) x = x - incrementor
            we_startpoint = x
            //System.out.println("WS " + we_startpoint);
            var ws_stoppoint = ceil(east)
            x = -180.0
            while (x < ws_stoppoint) x = x + incrementor
            if (we_startpoint < -180) {
                we_startpoint = -180.0
            }
            if (ws_stoppoint > 180) {
                ws_stoppoint = 180.0
            }
            return doubleArrayOf(ws_stoppoint, we_startpoint)
        } else {
            //hmm start at origin, add inc until we go too far, then back off, go to the next zoom level
            var west_start_point = -180.0
            if (west > 0) {
                west_start_point = 0.0
            }
            var easter_stop_point = 180.0
            if (east < 0) {
                easter_stop_point = 0.0
            }

            for (xx in 2..zoom) {
                val inc = getIncrementor(xx)
                while (easter_stop_point > east + inc) {
                    easter_stop_point -= inc
                    //System.out.println("east " + easter_stop_point);
                }

                while (west_start_point < west - inc) {
                    west_start_point += inc
                    if (DEBUG) {
                        println("west " + west_start_point)
                    }
                }
            }
            if (DEBUG) {
                println("return EW set as " + west_start_point + " " + easter_stop_point)
            }
            return doubleArrayOf(easter_stop_point, west_start_point)
        }
    }

    /**
     * this gets the distance in decimal degrees in between each line on the grid based on zoom level.
     * i had had it at more logical increments (90, 45, 30, etc) but changing to factors of 90 helps visualization
     * (i.e. when you zoom in on a particular crosshair, the crosshair is still there at the next zoom level, for the most part
     *
     * @param zoom mapview's osm zoom level
     * @return a double indicating the distance in degrees/decimal from which to place the gridlines on screen
     */
    private fun getIncrementor(zoom: Int): Double {
        when (zoom) {
            0, 1 -> return 30.0 * multiplier
            2 -> return 15.0 * multiplier
            3 -> return 9.0 * multiplier
            4 -> return 6.0 * multiplier
            5 -> return 3.0 * multiplier
            6 -> return 2.0 * multiplier
            7 -> return 1.0 * multiplier
            8 -> return 0.5 * multiplier
            9 -> return 0.25 * multiplier
            10 -> return 0.1 * multiplier
            11 -> return 0.05 * multiplier
            12 -> return 0.025 * multiplier
            13 -> return 0.0125 * multiplier
            14 -> return 0.00625 * multiplier
            15 -> return 0.003125 * multiplier
            16 -> return 0.0015625 * multiplier
            17 -> return 0.00078125 * multiplier
            18 -> return 0.000390625 * multiplier
            19 -> return 0.0001953125 * multiplier
            20 -> return 0.00009765625 * multiplier
            21 -> return 0.000048828125 * multiplier
            else -> return 0.0000244140625 * multiplier
        }
    }

    /**
     * resets the settings
     *
     * @since 5.6.3
     */
    fun setDefaults() {
        lineColor = Color.BLACK
        fontColor = Color.WHITE
        backgroundColor = Color.BLACK
        lineWidth = 1f
        fontSizeDp = 32
        DEBUG = false
        DEBUG2 = false
    }
}

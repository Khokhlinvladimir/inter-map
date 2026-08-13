package org.osmdroid.samplefragments.milstd2525

import android.graphics.Canvas
import android.util.SparseArray
import armyc2.c2sd.renderer.utilities.Color
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.PointReducer
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import sec.web.render.SECWebRenderer

/**
 * This overlay does a few things that are unique to milstd graphics
 *
 *
 * The GeoPoints provided are the symbol's control points. These control
 * points are then converted into the graphic and then added to the map using a
 * folder overlay + markers, polylines and polygons.
 *
 *
 *
 *
 * created on 1/30/2018.
 *
 * @author Alex O'Ree
 */
class MilStdMultipointOverlay(var symbol: SimpleSymbol, var inputGeoPoints: ArrayList<GeoPoint>) : Overlay() {
    private val mCurrentMapRotation = 0f
    private val mCurrentMapZoom = 0.0
    private val mCurrentCenter: IGeoPoint? = null
    protected var lastOverlay: FolderOverlay? = null

    override fun draw(c: Canvas, map: MapView, shadow: Boolean) {
        if (shadow) return
        //prevent looping forever for rendering
        //get the bounds,zoom, and rotation. if it's different, proceed
        var render = false
        if (mCurrentMapRotation != map.getMapOrientation()) render = true
        if (mCurrentCenter == null) render = true
        else if (mCurrentCenter != map.mapCenter) render = true
        if (mCurrentMapZoom != map.zoomLevelDouble) render = true

        if (!render) return

        //ok we are going to make a new symbol
        val dm = map.getContext().getResources().getDisplayMetrics()
        val densityDpi = dm.densityDpi


        //remove the last plotted configuration


        //Log.d(IMapView.LOGTAG, "point size before " + inputGeoPoints.size());
        //get the screen bounds
        val boundingBox = requireNotNull(map.getBoundingBox())
        val latSpanDegrees = boundingBox.latitudeSpan
        //get the degree difference, divide by dpi
        val tolerance = latSpanDegrees / densityDpi

        //each degree on screen is represented by this many dip
        val controlPts = StringBuilder()
        //run the douglas pucker algorithm to reduce the points for performance reasons
        val inputGeoPoints = PointReducer.reduceWithTolerance(
            this.inputGeoPoints,
            tolerance
        )

        //Log.d(IMapView.LOGTAG, "point size after " + inputGeoPoints.size());
        for (iGeoPoint in inputGeoPoints) {
            controlPts.append(iGeoPoint.longitude).append(",").append(iGeoPoint.latitude).append(" ")
        }


        val id = "id" //TODO
        val name = symbol.symbolCode
        val description = symbol.description
        var symbolCode = requireNotNull(symbol.symbolCode)

        val controlPoints = controlPts.toString()
        val altitudeMode = "absolute"
        //the ground scale
        val scale = TileSystem.GroundResolution(map.mapCenter!!.latitude, map.zoomLevelDouble)
        //"lowerLeftX,lowerLeftY,upperRightX,upperRightY."
        val bbox = boundingBox.lonWest.toString() + "," +
                boundingBox.latSouth + "," +
                boundingBox.lonEast + "," +
                boundingBox.latNorth


        val modifiers = symbol.modifiers

        if (symbolCode.get(0) == 'G') {
            //set the echleon to something meaningful
            symbolCode = symbolCode.substring(0, 10) + "-F" + symbolCode.substring(12)
            symbolCode = symbolCode.substring(0, 3) + "P" + symbolCode.substring(4)
        }

        //TODO country code is index 13-14
        //TODO X is 15
        val attributes = SparseArray<String?>()

        //TODO user defined drawing overides
        // attributes.put(MilStdAttributes.LineColor, "ffff0000");
        val symStd = 0

        //produce the symbol
        val flot = SECWebRenderer.RenderMultiPointAsMilStdSymbol(
            id,
            name,
            description,
            symbolCode,
            controlPoints,
            altitudeMode,
            scale,
            bbox,
            modifiers,
            attributes,
            symStd
        )

        //convert the symbol into osmdroid's data structures
        if (lastOverlay != null) {
            lastOverlay!!.onDetach(map)
        }
        lastOverlay = FolderOverlay()
        for (i in flot.getSymbolShapes().indices) {
            val info = flot.getSymbolShapes().get(i)

            if (info != null) {
                if (info.getFillColor() != null) {
                    val polylines = info.getPolylines()
                    if (polylines != null) for (list in polylines) {
                        val line = Polygon(map)
                        val geoPoints: MutableList<GeoPoint> = ArrayList()
                        for (p in list) {
                            geoPoints.add(GeoPoint(p!!.getY(), p.getX()))
                        }
                        line.setPoints(geoPoints)
                        if (info.getLineColor() != null) line.getOutlinePaint().setColor(info.getLineColor().toInt())
                        if (info.getFillColor() != null) line.getFillPaint()!!.setColor(info.getFillColor().toInt())
                        line.getOutlinePaint().setStrokeWidth(flot.getLineWidth().toFloat())
                        line.setId(id)
                        line.setTitle(name)
                        line.setSubDescription(description)
                        line.setSnippet(symbolCode)
                        line.setVisible(true)
                        lastOverlay!!.items!!.add(line)
                    }
                } else {
                    val polylines = info.getPolylines()
                    if (polylines != null) for (list in polylines) {
                        val line = Polyline(map)
                        val geoPoints: MutableList<GeoPoint> = ArrayList()
                        for (p in list) {
                            geoPoints.add(GeoPoint(p!!.getY(), p.getX()))
                        }
                        line.setPoints(geoPoints)
                        if (info.getLineColor() != null) line.getOutlinePaint().setColor(info.getLineColor().toInt())
                        line.setGeodesic(true)
                        line.setId(id)
                        line.setTitle(name)
                        line.getOutlinePaint().setStrokeWidth(flot.getLineWidth().toFloat())
                        line.setSubDescription(description)
                        line.setSnippet(symbolCode)
                        line.setVisible(true)
                        lastOverlay!!.items!!.add(line)
                    }
                }
            }
        }
        for (i in flot.getModifierShapes().indices) {
            val info = flot.getModifierShapes().get(i)
            if (info != null) {
                if (info.getPolylines() != null) {
                    val polylines = info.getPolylines()
                    if (info.getFillColor() != null) {
                        for (list in polylines) {
                            val line = Polygon(map)
                            val geoPoints: MutableList<GeoPoint> = ArrayList()
                            for (p in list) {
                                geoPoints.add(GeoPoint(p!!.getY(), p.getX()))
                            }
                            line.setPoints(geoPoints)
                            if (info.getLineColor() != null) line.getOutlinePaint().setColor(info.getLineColor().toInt())
                            if (info.getFillColor() != null) line.getFillPaint()!!.setColor(info.getFillColor().toInt())
                            line.setId(id)
                            line.setTitle(name)
                            line.getOutlinePaint().setStrokeWidth(flot.getLineWidth().toFloat())
                            line.setSubDescription(description)
                            line.setSnippet(symbolCode)
                            line.setVisible(true)
                            lastOverlay!!.items!!.add(line)
                        }
                    } else {
                        //it's a line
                        for (list in polylines) {
                            val line = Polyline(map)
                            val geoPoints: MutableList<GeoPoint> = ArrayList()
                            for (p in list) {
                                geoPoints.add(GeoPoint(p!!.getY(), p.getX()))
                            }
                            line.setPoints(geoPoints)
                            line.getOutlinePaint().setStrokeWidth(flot.getLineWidth().toFloat())
                            if (info.getLineColor() != null) line.getOutlinePaint().setColor(info.getLineColor().toInt())
                            line.setGeodesic(true)
                            line.setVisible(true)
                            lastOverlay!!.items!!.add(line)
                        }
                    }
                } else {
                    //not a line or a polygon
                    val m = Marker(map)
                    m.textLabelBackgroundColor = Color.WHITE.toInt()
                    m.textLabelFontSize = 14
                    m.textLabelForegroundColor = Color.BLACK.toInt()
                    m.setTitle(info.getModifierString())
                    m.rotation = info.getModifierStringAngle().toFloat()
                    m.setTextIcon(info.getModifierString())
                    m.position = GeoPoint(info.getModifierStringPosition().getY(), info.getModifierStringPosition().getX())
                    lastOverlay!!.items!!.add(m)
                }
            }
        }

        lastOverlay!!.draw(c, map, false)
    }
}

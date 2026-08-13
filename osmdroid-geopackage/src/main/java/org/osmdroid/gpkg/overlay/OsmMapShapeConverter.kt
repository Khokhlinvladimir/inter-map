/**
 * The MIT License
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 * This code was sourced from the National Geospatial Intelligency Agency and was
 * originally licensed under the MIT license. It has been modified to support
 * osmdroid's APIs.
 *
 *
 * You can find the original code base here:
 * https://github.com/ngageoint/geopackage-android-map
 * https://github.com/ngageoint/geopackage-android
 */
package org.osmdroid.gpkg.overlay

import android.util.Log
import mil.nga.geopackage.GeoPackageException
import mil.nga.proj.Projection
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionTransform
import mil.nga.sf.CircularString
import mil.nga.sf.CompoundCurve
import mil.nga.sf.Curve
import mil.nga.sf.CurvePolygon
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryCollection
import mil.nga.sf.GeometryType
import mil.nga.sf.LineString
import mil.nga.sf.MultiLineString
import mil.nga.sf.MultiPoint
import mil.nga.sf.MultiPolygon
import mil.nga.sf.Point
import mil.nga.sf.PolyhedralSurface
import mil.nga.sf.TIN
import mil.nga.sf.Triangle
import org.osmdroid.api.IMapView
import org.osmdroid.library.R as OsmdroidR
import org.osmdroid.gpkg.overlay.features.MarkerOptions
import org.osmdroid.gpkg.overlay.features.MultiLatLng
import org.osmdroid.gpkg.overlay.features.MultiMarker
import org.osmdroid.gpkg.overlay.features.MultiPolyline
import org.osmdroid.gpkg.overlay.features.MultiPolylineOptions
import org.osmdroid.gpkg.overlay.features.OsmDroidMapShape
import org.osmdroid.gpkg.overlay.features.OsmMapShapeType
import org.osmdroid.gpkg.overlay.features.PolygonOptions
import org.osmdroid.gpkg.overlay.features.PolygonOrientation
import org.osmdroid.gpkg.overlay.features.PolylineOptions
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import kotlin.math.max


/**
 * created on 8/19/2017.
 *
 * @author Alex O'Ree
 */
/**
 * Provides conversions methods between geometry object and Google Maps Android
 * API v2 Shapes
 *
 * @author osbornb
 */
class OsmMapShapeConverter @JvmOverloads constructor(
    projection: Projection? = null, options: MarkerOptions? = null, polylineOptions: PolylineOptions? = null,
    polygonOptions: PolygonOptions? = null
) {
    /**
     * Get the projection
     *
     * @return
     */
    /**
     * Projection
     */
    val projection: Projection?

    /**
     * Transformation to WGS 84
     */
    private val toWgs84: ProjectionTransform?

    /**
     * Transformation from WGS 84
     */
    private val fromWgs84: ProjectionTransform?

    /**
     * Convert polygon exteriors to specified orientation
     */
    private val exteriorOrientation = PolygonOrientation.COUNTERCLOCKWISE

    /**
     * Convert polygon holes to specified orientation
     */
    private val holeOrientation = PolygonOrientation.CLOCKWISE

    private val makerOptions: MarkerOptions?
    private val polylineOptions: PolylineOptions?
    private val polygonOptions: PolygonOptions?

    /**
     * Constructor with specified projection, see
     *
     * @param projection
     */
    /**
     * Constructor
     *
     * @since 1.3.2
     */
    init {
        Log.i(IMapView.LOGTAG, "Geopackage support is BETA. Please report any issues")
        this.projection = projection
        this.polylineOptions = polylineOptions
        this.polygonOptions = polygonOptions
        this.makerOptions = options
        if (projection != null) {
            toWgs84 = projection
                .getTransformation(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong())
            val wgs84 = toWgs84.getToProjection()
            fromWgs84 = wgs84.getTransformation(projection)
        } else {
            toWgs84 = null
            fromWgs84 = null
        }
    }


    /**
     * Transform a projection point to WGS84
     *
     * @param point
     * @return
     */
    fun toWgs84(point: Point): Point {
        var point = point
        if (projection != null) {
            val transformedPoint = toWgs84!!.transform(point.getX(), point.getY())
            point = Point(transformedPoint[0], transformedPoint[1])
        }
        return point
    }

    /**
     * Transform a WGS84 point to the projection
     *
     * @param point
     * @return
     */
    fun toProjection(point: Point): Point {
        var point = point
        if (projection != null) {
            val transformedPoint = toWgs84!!.transform(point.getX(), point.getY())
            point = Point(transformedPoint[0], transformedPoint[1])
        }
        return point
    }

    /**
     * Convert a [Point] to a [GeoPoint]
     *
     * @param point
     * @return
     */
    fun toLatLng2(point: Point): GeoPoint {
        var point = point
        point = toWgs84(point)
        return GeoPoint(point.getY(), point.getX())
    }

    fun toLatLng(point: Point): GeoPoint {
        var point = point
        point = toWgs84(point)
        return GeoPoint(point.getY(), point.getX())
    }


    /**
     * Convert a [LineString] to a [PolylineOptions]
     *
     * @param lineString
     * @return
     */
    fun toPolyline(lineString: LineString): Polyline {
        val line = Polyline()
        if (polylineOptions != null) {
            line.setTitle(polylineOptions.title)
            line.getOutlinePaint().setColor(polylineOptions.color)
            line.setGeodesic(polylineOptions.isGeodesic)
            line.getOutlinePaint().setStrokeWidth(polylineOptions.width)
            line.setSubDescription(polylineOptions.subtitle)
        }

        val pts: MutableList<GeoPoint> = ArrayList()
        for (point in lineString.getPoints()) {
            val latLng = toLatLng(point)
            pts.add(latLng)
        }
        line.setPoints(pts)

        return line
    }


    /**
     * Convert a [Polygon] to a [PolygonOptions]
     *
     * @param polygon
     * @return
     */
    fun toPolygon(polygon: mil.nga.sf.Polygon): Polygon {
        val newPoloygon = Polygon()
        val pts: MutableList<GeoPoint> = ArrayList()
        val holes: MutableList<MutableList<GeoPoint>> = ArrayList()

        val rings = polygon.getRings()

        if (!rings.isEmpty()) {
            var z: Double? = null

            // Add the polygon points
            val polygonLineString = rings.get(0)
            for (point in polygonLineString.getPoints()) {
                val latLng = toLatLng(point)
                pts.add(latLng)
            }

            // Add the holes
            for (i in 1 until rings.size) {
                val hole = rings.get(i)
                val holeLatLngs: MutableList<GeoPoint> = ArrayList()
                for (point in hole.getPoints()) {
                    val latLng = toLatLng(point)
                    holeLatLngs.add(latLng)
                    if (point.hasZ()) {
                        z = if (z == null) point.getZ() else max(
                            z,
                            point.getZ()
                        )
                    }
                }
                holes.add(holeLatLngs)
            }
        }
        newPoloygon.setPoints(pts)
        newPoloygon.setHoles(holes)

        if (polygonOptions != null) {
            newPoloygon.getFillPaint()!!.setColor(polygonOptions.fillColor)
            newPoloygon.getOutlinePaint().setColor(polygonOptions.strokeColor)
            newPoloygon.getOutlinePaint().setStrokeWidth(polygonOptions.strokeWidth)
            newPoloygon.setTitle(polygonOptions.title)
        }

        return newPoloygon
    }

    /**
     * Convert a [CurvePolygon] to a [PolygonOptions]
     *
     * @param curvePolygon curve polygon
     * @return polygon options
     * @since 1.4.1
     */
    fun toCurvePolygon(curvePolygon: CurvePolygon<*>): Polygon {
        val polygonOptions = Polygon()
        val pts: MutableList<GeoPoint> = ArrayList()
        val rings = curvePolygon.getRings()
        val holes: MutableList<MutableList<GeoPoint>> = ArrayList()
        if (!rings.isEmpty()) {
            var z: Double? = null

            // Add the polygon points
            val curve = rings.get(0)
            if (curve is CompoundCurve) {
                val compoundCurve = curve
                for (lineString in compoundCurve.getLineStrings()) {
                    for (point in lineString.getPoints()) {
                        val latLng = toLatLng(point)
                        pts.add(latLng)
                    }
                }
            } else if (curve is LineString) {
                val lineString = curve
                for (point in lineString.getPoints()) {
                    val latLng = toLatLng(point)
                    pts.add(latLng)
                }
            } else {
                throw GeoPackageException(
                    "Unsupported Curve Type: "
                            + curve.javaClass.getSimpleName()
                )
            }


            // Add the holes
            for (i in 1 until rings.size) {
                val hole = rings.get(i)
                val holeLatLngs: MutableList<GeoPoint> = ArrayList()
                if (hole is CompoundCurve) {
                    val holeCompoundCurve = hole
                    for (holeLineString in holeCompoundCurve.getLineStrings()) {
                        for (point in holeLineString.getPoints()) {
                            val latLng = toLatLng(point)
                            holeLatLngs.add(latLng)
                        }
                    }
                } else if (hole is LineString) {
                    val holeLineString = hole
                    for (point in holeLineString.getPoints()) {
                        val latLng = toLatLng(point)
                        holeLatLngs.add(latLng)
                        if (point.hasZ()) {
                            z = if (z == null) point.getZ() else max(
                                z,
                                point.getZ()
                            )
                        }
                    }
                } else {
                    throw GeoPackageException(
                        "Unsupported Curve Hole Type: "
                                + hole.javaClass.getSimpleName()
                    )
                }
                holes.add(holeLatLngs)
            }
        }
        polygonOptions.setHoles(holes)
        polygonOptions.setPoints(pts)

        return polygonOptions
    }


    /**
     * Convert a [MultiPoint] to a [MultiLatLng]
     *
     * @param multiPoint
     * @return
     */
    fun toLatLngs(multiPoint: MultiPoint): MultiLatLng {
        val multiLatLng = MultiLatLng()

        for (point in multiPoint.getPoints()) {
            val latLng = toLatLng2(point)
            multiLatLng.add(latLng)
        }

        return multiLatLng
    }


    /**
     * Convert a [MultiLineString] to a [MultiPolylineOptions]
     *
     * @param multiLineString
     * @return
     */
    fun toPolylines(multiLineString: MultiLineString): MutableList<Polyline> {
        val lines: MutableList<Polyline> = ArrayList<Polyline>()

        for (lineString in multiLineString.getLineStrings()) {
            val polyline = toPolyline(lineString)
            lines.add(polyline)
        }

        return lines
    }


    /**
     * Convert a [MultiPolygon] to a [Polygon]
     *
     * @param multiPolygon
     * @return
     */
    fun toPolygons(multiPolygon: MultiPolygon): MutableList<Polygon> {
        val polygons: MutableList<Polygon> = ArrayList<Polygon>()


        for (polygon in multiPolygon.getPolygons()) {
            val polygonOptions = toPolygon(polygon)
            polygons.add(polygonOptions)
        }

        return polygons
    }


    /**
     * Convert a [CompoundCurve] to a [MultiPolylineOptions]
     *
     * @param compoundCurve
     * @return
     */
    fun toPolylines(compoundCurve: CompoundCurve): MutableList<Polyline> {
        val lines: MutableList<Polyline> = ArrayList<Polyline>()
        val polylines = MultiPolylineOptions()

        for (lineString in compoundCurve.getLineStrings()) {
            val polyline = toPolyline(lineString)
            lines.add(polyline)
        }

        return lines
    }


    /**
     * Convert a [PolyhedralSurface] to a [Polygon]
     *
     * @param polyhedralSurface
     * @return
     */
    fun toPolygons(polyhedralSurface: PolyhedralSurface): MutableList<Polygon> {
        val polygons: MutableList<Polygon> = ArrayList<Polygon>()

        for (polygon in polyhedralSurface.getPolygons()) {
            val polygon1 = toPolygon(polygon)
            polygons.add(polygon1)
        }

        return polygons
    }

    /**
     * Convert a [Geometry] to a Map shape
     *
     * @param geometry
     * @return
     */
    fun toShape(geometry: Geometry): OsmDroidMapShape {
        var shape: OsmDroidMapShape? = null

        val geometryType = geometry.getGeometryType()
        when (geometryType) {
            GeometryType.POINT -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.LAT_LNG, toLatLng(geometry as Point)
            )

            GeometryType.LINESTRING -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYLINE_OPTIONS,
                toPolyline(geometry as LineString)
            )

            GeometryType.POLYGON -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYGON_OPTIONS,
                toPolygon(geometry as mil.nga.sf.Polygon)
            )

            GeometryType.MULTIPOINT -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_LAT_LNG,
                toLatLngs(geometry as MultiPoint)
            )

            GeometryType.MULTILINESTRING -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYLINE_OPTIONS,
                toPolylines(geometry as MultiLineString)
            )

            GeometryType.MULTIPOLYGON -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYGON_OPTIONS,
                toPolygons(geometry as MultiPolygon)
            )

            GeometryType.CIRCULARSTRING -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYLINE_OPTIONS,
                toPolyline(geometry as CircularString)
            )

            GeometryType.COMPOUNDCURVE -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYLINE_OPTIONS,
                toPolylines(geometry as CompoundCurve)
            )

            GeometryType.CURVEPOLYGON -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYGON_OPTIONS,
                toCurvePolygon(geometry as CurvePolygon<*>)
            )

            GeometryType.POLYHEDRALSURFACE -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYGON_OPTIONS,
                toPolygons(geometry as PolyhedralSurface)
            )

            GeometryType.TIN -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYGON_OPTIONS,
                toPolygons(geometry as TIN)
            )

            GeometryType.TRIANGLE -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYGON_OPTIONS,
                toPolygon(geometry as Triangle)
            )

            GeometryType.GEOMETRYCOLLECTION -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.COLLECTION,
                toShapes(geometry as GeometryCollection<Geometry>)
            )

            else -> throw GeoPackageException(
                "Unsupported Geometry Type: "
                        + geometryType.getName()
            )
        }

        return shape
    }

    /**
     * Convert a [GeometryCollection] to a list of Map shapes
     *
     * @param geometryCollection
     * @return
     */
    fun toShapes(
        geometryCollection: GeometryCollection<Geometry>
    ): MutableList<OsmDroidMapShape?> {
        val shapes: MutableList<OsmDroidMapShape?> = ArrayList<OsmDroidMapShape?>()

        for (geometry in geometryCollection.getGeometries()) {
            val shape = toShape(geometry)
            shapes.add(shape)
        }

        return shapes
    }

    /**
     * Convert a [Geometry] to a Map shape and add it
     *
     * @param map
     * @param geometry
     * @return
     */
    fun addToMap(map: MapView, geometry: Geometry): OsmDroidMapShape {
        var shape: OsmDroidMapShape? = null

        val geometryType = geometry.getGeometryType()
        when (geometryType) {
            GeometryType.POINT -> shape = OsmDroidMapShape(
                geometryType, OsmMapShapeType.MARKER,
                addLatLngToMap(map, toLatLng2(geometry as Point))
            )

            GeometryType.LINESTRING -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYLINE, addPolylineToMap(
                    map,
                    toPolyline(geometry as LineString)
                )
            )

            GeometryType.POLYGON -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYGON, addPolygonToMap(
                    map,
                    toPolygon(geometry as mil.nga.sf.Polygon),
                    polygonOptions
                )
            )

            GeometryType.MULTIPOINT -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_MARKER, addLatLngsToMap(
                    map,
                    toLatLngs(geometry as MultiPoint)
                )
            )

            GeometryType.MULTILINESTRING -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYLINE, addPolylinesToMap(
                    map,
                    toPolylines(geometry as MultiLineString)
                )
            )

            GeometryType.MULTIPOLYGON -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYGON, addPolygonsToMap(
                    map,
                    toPolygons(geometry as MultiPolygon), polygonOptions
                )
            )

            GeometryType.CIRCULARSTRING -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYLINE, addPolylineToMap(
                    map,
                    toPolyline(geometry as CircularString)
                )
            )

            GeometryType.COMPOUNDCURVE -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYLINE, addPolylinesToMap(
                    map,
                    toPolylines(geometry as CompoundCurve)
                )
            )

            GeometryType.CURVEPOLYGON -> {
                val polygon = toCurvePolygon(geometry as CurvePolygon<*>)
                shape = OsmDroidMapShape(
                    geometryType,
                    OsmMapShapeType.POLYGON, addPolygonToMap(
                        map,
                        polygon, polygonOptions
                    )
                )
            }

            GeometryType.POLYHEDRALSURFACE -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYGON, addPolygonsToMap(
                    map,
                    toPolygons(geometry as PolyhedralSurface), polygonOptions
                )
            )

            GeometryType.TIN -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.MULTI_POLYGON, addPolygonsToMap(
                    map,
                    toPolygons(geometry as TIN), polygonOptions
                )
            )

            GeometryType.TRIANGLE -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.POLYGON, addPolygonToMap(
                    map,
                    toPolygon(geometry as Triangle), polygonOptions
                )
            )

            GeometryType.GEOMETRYCOLLECTION -> shape = OsmDroidMapShape(
                geometryType,
                OsmMapShapeType.COLLECTION, addToMap(
                    map,
                    geometry as GeometryCollection<Geometry>
                )
            )

            else -> throw GeoPackageException(
                "Unsupported Geometry Type: "
                        + geometryType.getName()
            )
        }

        return shape
    }

    /**
     * Convert a [GeometryCollection] to a list of Map shapes and add to
     * the map
     *
     * @param map
     * @param geometryCollection
     * @return
     */
    fun addToMap(
        map: MapView,
        geometryCollection: GeometryCollection<Geometry>
    ): MutableList<OsmDroidMapShape?> {
        val shapes: MutableList<OsmDroidMapShape?> = ArrayList<OsmDroidMapShape?>()

        for (geometry in geometryCollection.getGeometries()) {
            val shape = addToMap(map, geometry)
            shapes.add(shape)
        }

        return shapes
    }


    companion object {
        /**
         * Add a LatLng to the map
         *
         * @param map
         * @param latLng
         * @param options
         * @return
         */
        /**
         * Add a LatLng to the map
         *
         * @param map
         * @param latLng
         * @return
         */
        @JvmOverloads
        fun addLatLngToMap(
            map: MapView, latLng: GeoPoint,
            options: MarkerOptions? = MarkerOptions()
        ): Marker {
            val m = Marker(map)
            m.position = latLng
            if (options != null) {
                if (options.icon != null) {
                    m.icon = options.icon
                }
                m.alpha = options.alpha
                m.setTitle(options.title)
                m.setSubDescription(options.subdescription)
                m.setInfoWindow(BasicInfoWindow(OsmdroidR.layout.bonuspack_bubble, map))
            }
            map.getOverlayManager().add(m)
            return m
        }


        /**
         * Add a Polyline to the map
         *
         * @param map
         * @param polyline
         * @return
         */
        fun addPolylineToMap(
            map: MapView,
            polyline: Polyline
        ): Polyline {
            if (polyline.getInfoWindow() == null) polyline.setInfoWindow(BasicInfoWindow(OsmdroidR.layout.bonuspack_bubble, map))
            map.getOverlayManager().add(polyline)
            return polyline
        }

        /**
         * Add a Polygon to the map
         *
         * @param map
         * @return
         */
        fun addPolygonToMap(
            map: MapView,
            pts: List<GeoPoint>,
            holes: List<List<GeoPoint>>, options: PolygonOptions?
        ): Polygon {
            val polygon1 = Polygon(map)
            polygon1.setPoints(pts)
            polygon1.setHoles(holes)
            if (options != null) {
                polygon1.getFillPaint()!!.setColor(options.fillColor)
                polygon1.setTitle(options.title)
                polygon1.getOutlinePaint().setColor(options.strokeColor)
                polygon1.getOutlinePaint().setStrokeWidth(options.strokeWidth)
                polygon1.setSubDescription(options.subtitle)
                polygon1.setInfoWindow(BasicInfoWindow(OsmdroidR.layout.bonuspack_bubble, map))
            }


            map.getOverlayManager().add(polygon1)
            return polygon1
        }


        /**
         * Add a Polygon to the map
         *
         * @param map
         * @param polygon
         * @return
         */
        fun addPolygonToMap(
            map: MapView,
            polygon: Polygon, options: PolygonOptions?
        ): Polygon {
            if (options != null) {
                polygon.getFillPaint()!!.setColor(options.fillColor)
                polygon.setTitle(options.title)
                polygon.getOutlinePaint().setColor(options.strokeColor)
                polygon.getOutlinePaint().setStrokeWidth(options.strokeWidth)
                polygon.setSubDescription(options.subtitle)
                polygon.setInfoWindow(BasicInfoWindow(OsmdroidR.layout.bonuspack_bubble, map))
            }


            map.getOverlayManager().add(polygon)
            return polygon
        }


        /**
         * Add a list of LatLngs to the map
         *
         * @param map
         * @param latLngs
         * @return
         */
        fun addLatLngsToMap(map: MapView, latLngs: MultiLatLng): MultiMarker {
            val multiMarker = MultiMarker()
            for (latLng in latLngs.latLngs) {
                val marker: Marker = Companion.addLatLngToMap(map, latLng!!, latLngs.markerOptions)
                multiMarker.add(marker)
            }
            return multiMarker
        }

        /**
         * Add a list of Polylines to the map
         *
         * @param map
         * @param polylines
         * @return
         */
        fun addPolylinesToMap(
            map: MapView,
            polylines: MutableList<Polyline>
        ): MultiPolyline {
            val multiPolyline = MultiPolyline()

            for (line in polylines) {
                if (line.getInfoWindow() == null) line.setInfoWindow(BasicInfoWindow(OsmdroidR.layout.bonuspack_bubble, map))
                map.getOverlayManager().add(line)
                multiPolyline.add(line)
            }
            return multiPolyline
        }


        fun addPolygonsToMap(
            map: MapView, polygons: MutableList<Polygon>, opts: PolygonOptions?
        ): org.osmdroid.gpkg.overlay.features.MultiPolygon {
            val multiPolygon = org.osmdroid.gpkg.overlay.features.MultiPolygon()
            for (polygonOption in polygons) {
                val polygon: Polygon = addPolygonToMap(map, polygonOption.getActualPoints(), polygonOption.holes, opts)

                if (polygon.getInfoWindow() == null) polygon.setInfoWindow(BasicInfoWindow(OsmdroidR.layout.bonuspack_bubble, map))
                multiPolygon.add(polygon)
            }
            return multiPolygon
        }
    }
}

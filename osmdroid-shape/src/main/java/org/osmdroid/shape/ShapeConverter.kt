package org.osmdroid.shape

import android.util.Log
import net.iryndin.jdbf.core.DbfRecord
import net.iryndin.jdbf.reader.DbfReader
import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader
import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.PointData
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.MultiPointPlainShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PointShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolylineShape
import org.osmdroid.api.IMapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

/**
 * https://github.com/osmdroid/osmdroid/issues/906
 * A simple utility class to convert a shape file into osmdroid overlays
 * created on 1/28/2018.
 *
 * @author Alex O'Ree
 * @since 6.1.0
 */
object ShapeConverter {
    /**
     * @param map             the MapView to which these overlays will be added.
     * @param file            the shape file to be converted.
     * @param prefs           allows the client to relax the level of validation when reading a shape file.
     * @param shapeMetaSetter customize titles, snippets, sub-descriptions of bubbles, and paint of overlays.
     * @return an arraylist of all overlays from the shapefile.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun convert(map: MapView?, file: File, prefs: ValidationPreferences, shapeMetaSetter: ShapeMetaSetter): MutableList<Overlay?> {
        val folder: MutableList<Overlay?> = ArrayList<Overlay?>()

        var `is`: FileInputStream? = null
        var dbfInputStream: FileInputStream? = null
        var dbfReader: DbfReader? = null
        var r: ShapeFileReader? = null

        try {
            val dbase = File(file.getParentFile(), file.getName().replace(".shp", ".dbf"))
            if (dbase.exists()) {
                dbfInputStream = FileInputStream(dbase)
                dbfReader = DbfReader(dbfInputStream)
            }
            `is` = FileInputStream(file)
            r = ShapeFileReader(`is`, prefs)


            while (true) {
                val s: AbstractShape = r.next() ?: break
                var metadata: DbfRecord? = null
                if (dbfReader != null) metadata = dbfReader.read()

                when (s.shapeType) {
                    ShapeType.POINT -> {
                        val aPoint = s as PointShape
                        val m = Marker(map)
                        m.setPosition(fixOutOfRange(GeoPoint(aPoint.y, aPoint.x)))

                        shapeMetaSetter.set(metadata, m)

                        folder.add(m)
                    }

                    ShapeType.POLYGON -> {
                        val aPolygon = s as PolygonShape


                        var i = 0
                        while (i < aPolygon.numberOfParts) {
                            val polygon = Polygon(map)

                            val points: Array<PointData?> = aPolygon.getPointsOfPart(i)
                            val pts: MutableList<GeoPoint?> = ArrayList<GeoPoint?>()

                            for (nullablePoint in points) {
                                val p = requireNotNull(nullablePoint)
                                val pt = fixOutOfRange(GeoPoint(p.y, p.x))
                                pts.add(pt)
                            }
                            pts.add(pts.get(0)) //force the polygon to close

                            polygon.setPoints(pts)

                            shapeMetaSetter.set(metadata, polygon)

                            folder.add(polygon)
                            i++
                        }
                    }

                    ShapeType.POLYLINE -> {
                        val polylineShape = s as PolylineShape
                        var i = 0
                        while (i < polylineShape.numberOfParts) {
                            val line = Polyline(map)

                            val points: Array<PointData?> = polylineShape.getPointsOfPart(i)
                            val pts: MutableList<GeoPoint?> = ArrayList<GeoPoint?>()

                            for (nullablePoint in points) {
                                val p = requireNotNull(nullablePoint)
                                val pt = fixOutOfRange(GeoPoint(p.y, p.x))
                                pts.add(pt)
                            }

                            line.setPoints(pts)

                            shapeMetaSetter.set(metadata, line)

                            folder.add(line)
                            i++
                        }
                    }

                    ShapeType.MULTIPOINT -> {
                        val aPoint = s as MultiPointPlainShape

                        val points: Array<PointData?> = aPoint.points
                        for (nullablePoint in points) {
                            val p = requireNotNull(nullablePoint)
                            val m = Marker(map)
                            m.setPosition(fixOutOfRange(GeoPoint(p.y, p.x)))

                            shapeMetaSetter.set(metadata, m)

                            folder.add(m)
                        }
                    }

                    else -> Log.w(IMapView.LOGTAG, s.shapeType.toString() + " was unhandled! " + s.javaClass.getCanonicalName())
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            try {
                `is`!!.close()
            } catch (ex: Exception) {
            }
            try {
                dbfReader!!.close()
            } catch (ex: Exception) {
            }
            try {
                dbfInputStream!!.close()
            } catch (ex: Exception) {
            }
        }
        return folder
    }

    val defaultValidationPreferences: ValidationPreferences
        get() {
            val pref = ValidationPreferences()
            pref.maxNumberOfPointsPerShape = 200000
            return pref
        }

    @JvmOverloads
    @Throws(Exception::class)
    fun convert(map: MapView?, file: File, pref: ValidationPreferences = defaultValidationPreferences): MutableList<Overlay?> {
        return convert(map, file, pref, DefaultShapeMetaSetter())
    }

    private fun fixOutOfRange(point: GeoPoint): GeoPoint {
        if (point.latitude > 90.00) point.setLatitude(90.00)
        else if (point.latitude < -90.00) point.setLatitude(-90.00)

        if (abs(point.longitude) > 180.00) {
            var longitude = point.longitude
            val diff = (if (longitude > 0) -360 else 360).toDouble()
            while (abs(longitude) > 180) longitude += diff
            point.setLongitude(longitude)
        }

        return point
    }
}

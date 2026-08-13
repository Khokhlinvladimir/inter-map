package manualtests

import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.MultiPointPlainShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolylineShape
import java.io.FileInputStream
import java.io.IOException

object TestShapeFileReader {
    @Throws(IOException::class, InvalidShapeFileException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val filename: String?

        //		 filename = "testdata/freeworld/10m-coastline/10m_coastline.shp";

//		 filename = "testdata/freefiles/pointfiles/prop_text.shp";
//		 filename = "testdata/freefiles/pointfiles/roadtext.shp";
//		 filename = "testdata/freefiles/pointfiles/sbuild.shp";

//		 filename = "testdata/freefiles/polygonfiles/lbuild.shp";
//		 filename = "testdata/freefiles/polygonfiles/property.shp";
//		 filename = "testdata/freefiles/polygonfiles/subdiv.shp";
//		 filename = "testdata/freefiles/polygonfiles/water.shp";

//		 filename = "testdata/freefiles/polylinefiles/roadcl.shp";
//		 filename = "testdata/freefiles/polylinefiles/roadeop.shp";
        filename = "src/test/resources/freefiles/multipoint/admin_font_point.shp"

        // filename =
        // "data/testdata/badfiles/multipoint-markedas-multipointz.shp";

        // filename ="";
        // filename ="";
        // filename ="";
        val `is` = FileInputStream(filename)

        //		 ValidationPreferences prefs = new ValidationPreferences();
//		 prefs.setMaxNumberOfPointsPerShape(16650);
//		 prefs.setForceType(ShapeType.MULTIPOINT);
//		 ShapeFileReader r = new ShapeFileReader(is, prefs);
        val r = ShapeFileReader(`is`)

        val h = r.getHeader()
        display("header: " + h)

        val counters: MutableMap<ShapeType?, Int?> = HashMap<ShapeType?, Int?>()

        // for (ShapeType t : ShapeType.values()) {
        // counters.put(t, 0);
        // }
        val maxShapes = 1000000000
        var total = 0
        var totalPoints = 0
        var totalParts = 0
        while (total < maxShapes) {
            val shape: AbstractShape = r.next() ?: break
            if (shape.shapeType == ShapeType.POLYLINE) {
                val ps = shape as PolylineShape
                if (ps.numberOfParts > 1) {
                    display(
                        (total.toString() + ": " + shape.shapeType + " (parts:"
                                + ps.numberOfParts + ", points:"
                                + ps.numberOfPoints + ")")
                    )
                    for (i in 0 until ps.numberOfParts) {
                        val points = ps.getPointsOfPart(i)
                        display(
                            ("- part " + i + " (" + points.size
                                    + " points)")
                        )
                    }
                }
                totalParts += ps.numberOfParts
                totalPoints += ps.numberOfPoints
            } else {
                display(
                    (shape.header!!.recordNumber
                        .toString() + " ["
                            + shape.header!!.contentLength
                            + " w]: "
                            + shape.shapeType
                            + " ("
                            + shape.shapeType!!.id
                            + ")"
                            + (if (shape.shapeType == ShapeType.MULTIPOINT_Z) (" - "
                            + (shape as MultiPointPlainShape)
                        .numberOfPoints + " points") else ""))
                )
            }

            val typeCounter = counters[shape.shapeType]
            if (typeCounter == null) {
                counters[shape.shapeType] = 1
            } else {
                counters[shape.shapeType] = 1 + typeCounter
            }

            total++
        }
        display("=========================")
        display("Total points: " + totalPoints + ", total parts: " + totalParts)
        display("")
        for (t in counters.keys) {
            display("  " + counters.get(t) + " " + t)
        }
        display("  ==============")
        display("  " + total + " Total shapes")

        `is`.close()
    }

    private fun display(txt: String?) {
        println(txt)
    }
}

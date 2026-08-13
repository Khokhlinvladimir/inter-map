package tests

import junit.framework.Assert
import org.junit.Test
import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader
import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

class ReaderTests {
    @Test
    @Throws(IOException::class, InvalidShapeFileException::class)
    fun testGoodFiles() {
        checkSimpleCase(
            "src/test/resources/freefiles/pointfiles/prop_text.shp",
            ShapeType.POINT, 1897
        )
        checkSimpleCase(
            "src/test/resources/freefiles/pointfiles/roadtext.shp",
            ShapeType.POINT, 184
        )
        checkSimpleCase(
            "src/test/resources/freefiles/pointfiles/sbuild.shp", ShapeType.POINT,
            1592
        )

        checkSimpleCase(
            "src/test/resources/freefiles/polygonfiles/lbuild.shp",
            ShapeType.POLYGON, 27
        )
        checkSimpleCase(
            "src/test/resources/freefiles/polygonfiles/property.shp",
            ShapeType.POLYGON, 1650
        )
        checkSimpleCase(
            "src/test/resources/freefiles/polygonfiles/subdiv.shp",
            ShapeType.POLYGON, 29
        )
        checkSimpleCase(
            "src/test/resources/freefiles/polygonfiles/water.shp",
            ShapeType.POLYGON, 3
        )

        checkSimpleCase(
            "src/test/resources/freefiles/polylinefiles/roadcl.shp",
            ShapeType.POLYLINE, 231
        )
        checkSimpleCase(
            "src/test/resources/freefiles/polylinefiles/roadeop.shp",
            ShapeType.POLYLINE, 458
        )

        checkSimpleCase(
            "src/test/resources/freefiles/multipoint/admin_font_point.shp",
            ShapeType.MULTIPOINT, 2175
        )
    }

    @Test
    @Throws(FileNotFoundException::class, IOException::class, InvalidShapeFileException::class)
    fun testBigFile() {
        try {
            readFile(
                "src/test/resources/freeworld/10m-coastline/10m_coastline.shp", null,
                ShapeType.POLYLINE, 4177
            )
            Assert.fail()
        } catch (e: InvalidShapeFileException) {
            // OK. The file exceeds 10000 shapes.
        }

        val prefs = ValidationPreferences()
        prefs.maxNumberOfPointsPerShape = 16650
        readFile(
            "src/test/resources/freeworld/10m-coastline/10m_coastline.shp", prefs,
            ShapeType.POLYLINE, 4177
        )
    }

    @Test
    @Throws(FileNotFoundException::class, IOException::class, InvalidShapeFileException::class)
    fun testRecoverableBadFile() {
        try {
            readFile(
                "src/test/resources/freefiles/badfiles/multipointm-marked-as-multipointz.shp",
                null, ShapeType.MULTIPOINT_Z, 300
            )
            Assert.fail()
        } catch (e: InvalidShapeFileException) {
            // OK
        }

        val prefs = ValidationPreferences()
        prefs.forceShapeType = ShapeType.MULTIPOINT_M
        readFile(
            "src/test/resources/freefiles/badfiles/multipointm-marked-as-multipointz.shp",
            prefs, ShapeType.MULTIPOINT_M, 312
        )
    }

    // Utils
    @Throws(IOException::class, InvalidShapeFileException::class)
    private fun checkSimpleCase(
        filename: String?,
        expectedShapeType: ShapeType?, expectedNumberOfShapes: Int
    ) {
        readFile(filename, null, expectedShapeType, expectedNumberOfShapes)
    }

    @Throws(FileNotFoundException::class, IOException::class, InvalidShapeFileException::class)
    private fun readFile(
        filename: String?,
        prefs: ValidationPreferences?, expectedShapeType: ShapeType?,
        expectedNumberOfShapes: Int
    ) {
        var `is`: FileInputStream? = null
        try {
            `is` = FileInputStream(filename)
            val r: ShapeFileReader?
            if (prefs == null) {
                r = ShapeFileReader(`is`)
            } else {
                r = ShapeFileReader(`is`, prefs)
            }

            val header = r.getHeader()
            Assert.assertEquals(expectedShapeType, header.shapeType)

            var actualNumberOfShapes = 0
            var s: AbstractShape?
            while ((r.next().also { s = it }) != null) {
                actualNumberOfShapes++
            }
            Assert.assertEquals(expectedNumberOfShapes, actualNumberOfShapes)
        } finally {
            if (`is` != null) {
                `is`.close()
            }
        }
    }
}

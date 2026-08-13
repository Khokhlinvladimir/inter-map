package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.Const
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.InputStream

/**
 * Represents a MultiPointZ Shape object, as defined by the ESRI Shape file
 * specification.
 */
class MultiPointZShape(
    shapeHeader: ShapeHeader?,
    shapeType: ShapeType?, `is`: InputStream,
    rules: ValidationPreferences
) : AbstractMultiPointShape(shapeHeader, shapeType, `is`, rules) {
    // Getters
    val minZ: Double
    val maxZ: Double
    val z: DoubleArray

    val minM: Double
    val maxM: Double
    val m: DoubleArray

    init {
        if (!rules.isAllowBadContentLength) {
            val expectedLength: Int = (BASE_CONTENT_LENGTH
                    + (this.numberOfPoints * (8 * 2 + 8 + 8)) / 2)
            if (this.header!!.contentLength != expectedLength) {
                throw InvalidShapeFileException(
                    ("Invalid " + shapeTypeName
                            + " shape header's content length. " + "Expected " + expectedLength
                            + " 16-bit words (for " + this.numberOfPoints + " points)"
                            + " but found " + this.header!!.contentLength + ". "
                            + Const.PREFERENCES)
                )
            }
        }

        this.minZ = ISUtil.readLeDouble(`is`)
        this.maxZ = ISUtil.readLeDouble(`is`)

        this.z = DoubleArray(this.numberOfPoints)
        for (i in 0 until this.numberOfPoints) {
            this.z[i] = ISUtil.readLeDouble(`is`)
        }

        this.minM = ISUtil.readLeDouble(`is`)
        this.maxM = ISUtil.readLeDouble(`is`)

        this.m = DoubleArray(this.numberOfPoints)
        for (i in 0 until this.numberOfPoints) {
            this.m[i] = ISUtil.readLeDouble(`is`)
        }
    }

    override val shapeTypeName: String?
        get() = "MultiPointZ"

    companion object {
        private val BASE_CONTENT_LENGTH = (4 + 8 * 4 + 4 + 8 * 2 + 8 * 2) / 2
    }
}

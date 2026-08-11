package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.Const
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.InputStream

/**
 * Represents a PointZ Shape object, as defined by the ESRI Shape file
 * specification.
 */
class PointZShape(
    shapeHeader: ShapeHeader?, shapeType: ShapeType?,
    `is`: InputStream, rules: ValidationPreferences
) : AbstractPointShape(shapeHeader, shapeType, `is`, rules) {
    // Getters
    val z: Double
    val m: Double

    init {
        if (!rules.isAllowBadContentLength
            && this.header!!.contentLength != FIXED_CONTENT_LENGTH
        ) {
            throw InvalidShapeFileException(
                ("Invalid PointZ shape header's content length. " + "Expected "
                        + FIXED_CONTENT_LENGTH + " 16-bit words but found "
                        + this.header!!.contentLength + ". " + Const.PREFERENCES)
            )
        }

        this.z = ISUtil.readLeDouble(`is`)
        this.m = ISUtil.readLeDouble(`is`)
    }

    companion object {
        private val FIXED_CONTENT_LENGTH = (4 + 8 + 8 + 8 + 8) / 2
    }
}

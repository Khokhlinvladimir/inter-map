package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.Const
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import java.io.InputStream

/**
 * Represents a Null Shape object, as defined by the ESRI Shape file
 * specification.
 */
class NullShape(
    shapeHeader: ShapeHeader?, shapeType: ShapeType?,
    `is`: InputStream, rules: ValidationPreferences
) : AbstractShape(shapeHeader, shapeType, `is`, rules) {
    init {
        if (!rules.isAllowBadContentLength
            && this.header!!.contentLength != FIXED_CONTENT_LENGTH
        ) {
            throw InvalidShapeFileException(
                ("Invalid Null shape header's content length. " + "Expected "
                        + FIXED_CONTENT_LENGTH + " 16-bit words but found "
                        + this.header!!.contentLength + ". " + Const.PREFERENCES)
            )
        }
    }

    companion object {
        private val FIXED_CONTENT_LENGTH = (4) / 2
    }
}

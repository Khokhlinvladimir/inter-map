package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.Const
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import java.io.InputStream

abstract class AbstractPolyPlainShape(
    shapeHeader: ShapeHeader?,
    shapeType: ShapeType?, `is`: InputStream,
    rules: ValidationPreferences
) : AbstractPolyShape(shapeHeader, shapeType, `is`, rules) {
    init {
        if (!rules.isAllowBadContentLength) {
            val expectedLength: Int = (BASE_CONTENT_LENGTH //
                    + (this.numberOfParts * (4)) / 2 //
                    + (this.numberOfPoints * (8 * 2)) / 2)
            if (this.header!!.contentLength != expectedLength) {
                throw InvalidShapeFileException(
                    ("Invalid " + shapeTypeName
                            + " shape header's content length. " + "Expected " + expectedLength
                            + " 16-bit words (for " + this.numberOfParts + " parts and "
                            + this.numberOfPoints + " points)" + " but found "
                            + this.header!!.contentLength + ". " + Const.PREFERENCES)
                )
            }
        }
    } // Getters

    companion object {
        private val BASE_CONTENT_LENGTH = (4 + 8 * 4 + 4 + 4) / 2
    }
}

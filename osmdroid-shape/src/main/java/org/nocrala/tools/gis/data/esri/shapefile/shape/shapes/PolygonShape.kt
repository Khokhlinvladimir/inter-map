package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import java.io.InputStream

/**
 * Represents a Polygon Shape object, as defined by the ESRI Shape file
 * specification.
 */
class PolygonShape(
    shapeHeader: ShapeHeader?, shapeType: ShapeType?,
    `is`: InputStream, rules: ValidationPreferences
) : AbstractPolyPlainShape(shapeHeader, shapeType, `is`, rules) {
    override val shapeTypeName: String
        get() = "Polygon"
}

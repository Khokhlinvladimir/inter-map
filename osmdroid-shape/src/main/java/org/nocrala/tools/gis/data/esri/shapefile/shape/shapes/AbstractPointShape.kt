package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.InputStream

abstract class AbstractPointShape(
    shapeHeader: ShapeHeader?,
    shapeType: ShapeType?, `is`: InputStream,
    rules: ValidationPreferences
) : AbstractShape(shapeHeader, shapeType, `is`, rules) {
    // Getters
    @JvmField
    val x: Double
    @JvmField
    val y: Double

    init {
        this.x = ISUtil.readLeDouble(`is`)
        this.y = ISUtil.readLeDouble(`is`)
    }
}

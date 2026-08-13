package org.nocrala.tools.gis.data.esri.shapefile.shape

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import java.io.InputStream

abstract class AbstractShape(// Getters
    @JvmField var header: ShapeHeader?,
    @JvmField var shapeType: ShapeType?, `is`: InputStream,
    rules: ValidationPreferences
)

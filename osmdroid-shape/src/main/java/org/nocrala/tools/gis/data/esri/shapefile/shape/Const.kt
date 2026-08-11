package org.nocrala.tools.gis.data.esri.shapefile.shape

import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader

object Const {
    @JvmField
    val PREFERENCES: String = ("You can change the validation preferences using "
            + "the additional constructor of the "
            + ShapeFileReader::class.java.getName()
            + " class.")
}

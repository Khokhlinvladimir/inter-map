package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.Const
import org.nocrala.tools.gis.data.esri.shapefile.shape.PointData
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.InputStream

abstract class AbstractMultiPointShape(
    shapeHeader: ShapeHeader?,
    shapeType: ShapeType?, `is`: InputStream,
    rules: ValidationPreferences
) : AbstractShape(shapeHeader, shapeType, `is`, rules) {
    // Getters
    var boxMinX: Double
        protected set
    var boxMinY: Double
        protected set
    var boxMaxX: Double
        protected set
    var boxMaxY: Double
        protected set

    var numberOfPoints: Int
        protected set
    var points: Array<PointData?>
        protected set

    init {
        this.boxMinX = ISUtil.readLeDouble(`is`)
        this.boxMinY = ISUtil.readLeDouble(`is`)
        this.boxMaxX = ISUtil.readLeDouble(`is`)
        this.boxMaxY = ISUtil.readLeDouble(`is`)

        this.numberOfPoints = ISUtil.readLeInt(`is`)

        if (!rules.isAllowUnlimitedNumberOfPointsPerShape) {
            if (this.numberOfPoints > rules.maxNumberOfPointsPerShape) {
                throw InvalidShapeFileException(
                    ("Invalid " + this.shapeTypeName
                            + " shape number of points. "
                            + "The allowed maximum number of points was "
                            + rules.maxNumberOfPointsPerShape + " but found "
                            + this.numberOfPoints + ". " + Const.PREFERENCES)
                )
            }
        }

        this.points = arrayOfNulls<PointData>(this.numberOfPoints)
        for (i in 0 until this.numberOfPoints) {
            val x = ISUtil.readLeDouble(`is`)
            val y = ISUtil.readLeDouble(`is`)
            this.points[i] = PointData(x, y)
        }
    }

    protected abstract val shapeTypeName: String?
}

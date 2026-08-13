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
import java.util.Arrays

abstract class AbstractPolyShape(
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

    var numberOfParts: Int
        protected set
    var numberOfPoints: Int
        protected set

    var partFirstPoints: IntArray
        protected set
    var points: Array<PointData?>
        protected set

    init {
        this.boxMinX = ISUtil.readLeDouble(`is`)
        this.boxMinY = ISUtil.readLeDouble(`is`)
        this.boxMaxX = ISUtil.readLeDouble(`is`)
        this.boxMaxY = ISUtil.readLeDouble(`is`)

        this.numberOfParts = ISUtil.readLeInt(`is`)

        if (this.numberOfParts < 0) {
            throw InvalidShapeFileException(
                ("Invalid " + this.shapeTypeName
                        + " shape number of parts. "
                        + "It should be a number greater than zero, but found "
                        + this.numberOfParts + ". " + Const.PREFERENCES)
            )
        }

        this.numberOfPoints = ISUtil.readLeInt(`is`)

        if (this.numberOfPoints < 0) {
            throw InvalidShapeFileException(
                ("Invalid " + this.shapeTypeName
                        + " shape number of points. "
                        + "It should be a number greater than zero, but found "
                        + this.numberOfPoints + ". " + Const.PREFERENCES)
            )
        }

        if (this.numberOfParts > this.numberOfPoints) {
            throw InvalidShapeFileException(
                ("Invalid " + this.shapeTypeName
                        + " shape number of parts. "
                        + "It should be smaller or equal to the number of points ("
                        + this.numberOfPoints + "), but found " + this.numberOfParts + ". "
                        + Const.PREFERENCES)
            )
        }

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

        this.partFirstPoints = IntArray(this.numberOfParts)
        for (i in 0 until this.numberOfParts) {
            this.partFirstPoints[i] = ISUtil.readLeInt(`is`)
        }

        this.points = arrayOfNulls<PointData>(this.numberOfPoints)
        for (i in 0 until this.numberOfPoints) {
            val x = ISUtil.readLeDouble(`is`)
            val y = ISUtil.readLeDouble(`is`)
            this.points[i] = PointData(x, y)
        }
    }

    protected abstract val shapeTypeName: String?

    fun getPointsOfPart(i: Int): Array<PointData?> {
        if (i < 0 || i >= this.numberOfParts) {
            throw RuntimeException(
                ("Invalid part " + i + ". Available parts [0:"
                        + this.numberOfParts + "].")
            )
        }
        val from = this.partFirstPoints[i]
        val to = if (i < this.numberOfParts - 1)
            this.partFirstPoints[i + 1]
        else
            this.points.size

        if (from < 0 || from > this.points.size) {
            throw RuntimeException(
                ("Malformed content. Part start (" + from
                        + ") is out of range. Valid range of points is [0:"
                        + this.points.size + "].")
            )
        }

        if (to < 0 || to > this.points.size) {
            throw RuntimeException(
                ("Malformed content. Part end (" + to
                        + ") is out of range. Valid range of points is [0:"
                        + this.points.size + "].")
            )
        }

        return Arrays.copyOfRange<PointData?>(this.points, from, to)
    }
}

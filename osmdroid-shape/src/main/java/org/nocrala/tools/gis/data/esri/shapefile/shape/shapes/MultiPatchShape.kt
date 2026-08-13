package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.Const
import org.nocrala.tools.gis.data.esri.shapefile.shape.PartType
import org.nocrala.tools.gis.data.esri.shapefile.shape.PartType.Companion.parse
import org.nocrala.tools.gis.data.esri.shapefile.shape.PointData
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.InputStream
import java.util.Arrays

/**
 * Represents a MultiPatch Shape object, as defined by the ESRI Shape file
 * specification.
 */
class MultiPatchShape(
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

    var numberOfParts: Int = 0
        protected set
    var numberOfPoints: Int = 0
        protected set

    var partFirstPoints: IntArray
        protected set
    var partTypes: Array<PartType?>
        protected set
    var points: Array<PointData?>
        protected set

    val minZ: Double
    val maxZ: Double
    val z: DoubleArray

    val minM: Double
    val maxM: Double
    val m: DoubleArray

    init {
        if (!rules.isAllowBadContentLength) {
            val expectedLength: Int = (BASE_CONTENT_LENGTH //
                    + (this.numberOfParts * (4 + 4)) / 2 //
                    + (this.numberOfPoints * (8 * 2 + 8 + 8)) / 2)
            if (this.header!!.contentLength != expectedLength) {
                throw InvalidShapeFileException(
                    ("Invalid " + this.shapeTypeName
                            + " shape header's content length. " + "Expected " + expectedLength
                            + " 16-bit words (for " + this.numberOfParts + " parts and "
                            + this.numberOfPoints + " points)" + " but found "
                            + this.header!!.contentLength + ". " + Const.PREFERENCES)
                )
            }
        }

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

        this.partTypes = arrayOfNulls<PartType>(this.numberOfParts)
        for (i in 0 until this.numberOfParts) {
            val partTypeId = ISUtil.readLeInt(`is`)
            val partType = parse(partTypeId)
            if (rules.forcePartType == null) {
                if (partType == null) {
                    throw InvalidShapeFileException(
                        ("Invalid " + this.shapeTypeName
                                + " shape part type. " + "Part type code found was " + partTypeId
                                + ". " + Const.PREFERENCES)
                    )
                }
                this.partTypes[i] = partType
            } else {
                this.partTypes[i] = rules.forcePartType
            }
        }

        this.points = arrayOfNulls<PointData>(this.numberOfPoints)
        for (i in 0 until this.numberOfPoints) {
            val x = ISUtil.readLeDouble(`is`)
            val y = ISUtil.readLeDouble(`is`)
            this.points[i] = PointData(x, y)
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

    private val shapeTypeName: String
        get() = "MultiPatch"

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

    fun getMOfPart(i: Int): DoubleArray? {
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

        return Arrays.copyOfRange(this.m, from, to)
    }

    fun getZOfPart(i: Int): DoubleArray? {
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

        return Arrays.copyOfRange(this.z, from, to)
    }

    companion object {
        private val BASE_CONTENT_LENGTH = (4 + 8 * 4 + 4 + 4 + 8 * 2 + 8 * 2) / 2
    }
}

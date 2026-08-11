package org.nocrala.tools.gis.data.esri.shapefile.shape.shapes

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.Const
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.InputStream
import java.util.Arrays

abstract class AbstractPolyZShape(
    shapeHeader: ShapeHeader?,
    shapeType: ShapeType?, `is`: InputStream,
    rules: ValidationPreferences
) : AbstractPolyShape(shapeHeader, shapeType, `is`, rules) {
    // Accessors
    val minZ: Double
    val maxZ: Double
    val z: DoubleArray

    val minM: Double
    val maxM: Double
    val measures: DoubleArray

    init {
        if (!rules.isAllowBadContentLength) {
            val expectedLength: Int = (BASE_CONTENT_LENGTH //
                    + (this.numberOfParts * (4)) / 2 //
                    + (this.numberOfPoints * (8 * 2 + 8 + 8)) / 2)
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

        this.minZ = ISUtil.readLeDouble(`is`)
        this.maxZ = ISUtil.readLeDouble(`is`)

        this.z = DoubleArray(this.numberOfPoints)
        for (i in 0 until this.numberOfPoints) {
            this.z[i] = ISUtil.readLeDouble(`is`)
        }

        this.minM = ISUtil.readLeDouble(`is`)
        this.maxM = ISUtil.readLeDouble(`is`)

        this.measures = DoubleArray(this.numberOfPoints)
        for (i in 0 until this.numberOfPoints) {
            this.measures[i] = ISUtil.readLeDouble(`is`)
        }
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

        return Arrays.copyOfRange(this.measures, from, to)
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

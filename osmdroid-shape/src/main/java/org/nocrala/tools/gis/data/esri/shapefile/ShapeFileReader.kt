package org.nocrala.tools.gis.data.esri.shapefile

import org.nocrala.tools.gis.data.esri.shapefile.exception.DataStreamEOFException
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.header.ShapeFileHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeHeader
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType.Companion.parse
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.MultiPatchShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.MultiPointMShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.MultiPointPlainShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.MultiPointZShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.NullShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PointMShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PointShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PointZShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonMShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonZShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolylineMShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolylineShape
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolylineZShape
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * Reads an ESRI Shape File from an InputStream and provides its contents as
 * simple Java objects.
 */
class ShapeFileReader {
    private lateinit var `is`: BufferedInputStream
    private lateinit var rules: ValidationPreferences

    private lateinit var header: ShapeFileHeader
    private var eofReached = false

    // Constructors
    /**
     *
     *
     * Reads a Shape File from an InputStream using the default validation
     * preferences. The default validation preferences conforms strictly to the
     * ESRI ShapeFile specification.
     *
     *
     *
     *
     * The constructor will automatically read the header of the file. Thereafter,
     * use the method next() to read all shapes.
     *
     *
     * @param is the InputStream to be read.
     * @throws InvalidShapeFileException if the data is malformed, according to the ESRI ShapeFile
     * specification.
     * @throws IOException               if it's not possible to read from the InputStream.
     */
    constructor(`is`: InputStream) {
        val rules = ValidationPreferences()
        initialize(`is`, rules)
    }

    /**
     *
     *
     * Reads a Shape File from an InputStream using the specified validation
     * preferences. Use this constructor when you want to relax or change the
     * validation preferences.
     *
     *
     *
     *
     * The constructor will automatically read the header of the file. Thereafter,
     * use the method next() to read all shapes.
     *
     *
     * @param is          the InputStream to be read.
     * @param preferences Customized validation preferences.
     * @throws InvalidShapeFileException if the data is malformed, according to the specified preferences.
     * @throws IOException               if it's not possible to read from the InputStream.
     */
    constructor(
        `is`: InputStream,
        preferences: ValidationPreferences
    ) {
        initialize(`is`, preferences)
    }

    @Throws(IOException::class, InvalidShapeFileException::class)
    private fun initialize(
        `is`: InputStream,
        preferences: ValidationPreferences
    ) {
        if (`is` == null) {
            throw RuntimeException(
                "Must specify a non-null input stream to read from."
            )
        }
        if (preferences == null) {
            throw RuntimeException("Must specify non-null rules.")
        }
        this.`is` = BufferedInputStream(`is`)
        this.rules = preferences
        this.eofReached = false
        this.header = ShapeFileHeader(this.`is`, this.rules)
    }

    // Methods
    /**
     * Reads one shape from the InputStream.
     *
     * @return a shape object, or null when the end of the stream is reached. The
     * returned shape object will be of one of the following classes:
     *
     *  * NullShape,
     *  * PointShape,
     *  * PolylineShape,
     *  * PolygonShape,
     *  * MultiPointPlainShape,
     *  * PointZShape,
     *  * PolylineZShape,
     *  * PolygonZShape,
     *  * MultiPointZShape,
     *  * PointMShape,
     *  * PolylineMShape,
     *  * PolygonMShape,
     *  * MultiPointMShape,
     *  * or MultiPatchShape.
     *
     * The method getShapeType() of the AbstractShape object provides the
     * shape type, in order to to cast the object to the appropriate
     * class.
     * @throws InvalidShapeFileException if the data is malformed.
     * @throws IOException               if it's not possible to read from the InputStream.
     */
    @Throws(IOException::class, InvalidShapeFileException::class)
    fun next(): AbstractShape? {
        if (this.eofReached) {
            return null
        }

        this.rules.advanceOneRecordNumber()

        // Shape header
        val shapeHeader: ShapeHeader
        var shapeType: ShapeType? = null

        try {
            shapeHeader = ShapeHeader(this.`is`, this.rules)
        } catch (e: DataStreamEOFException) {
            this.eofReached = true
            return null
        }

        // Shape body
        try {
            val typeId = ISUtil.readLeInt(this.`is`)
            if (this.rules.forceShapeType != null) {
                shapeType = this.rules.forceShapeType
            } else {
                shapeType = parse(typeId)
                if (shapeType == null) {
                    throw InvalidShapeFileException(
                        ("Invalid shape type '" + typeId
                                + "'. " + "The shape type can be forced using "
                                + "the additional constructor with " + "ValidationRules.")
                    )
                }
                if (!this.rules.isAllowMultipleShapeTypes
                    && this.header.shapeType != shapeType
                ) {
                    throw InvalidShapeFileException(
                        ("Invalid shape type '"
                                + shapeType + "'. All included shapes must have the same "
                                + "type as the one specified on the file header ("
                                + this.header.shapeType
                                + "). This validation can be disabled using the "
                                + "additional constructor with ValidationRules.")
                    )
                }
            }
        } catch (e: EOFException) {
            throw InvalidShapeFileException(
                "Unexpected end of stream. "
                        + "The data is too short for the shape that was being read."
            )
        }

        try {
            when (shapeType) {
                ShapeType.NULL -> return NullShape(shapeHeader, shapeType, this.`is`, this.rules)

                ShapeType.POINT -> return PointShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.POLYLINE -> return PolylineShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.POLYGON -> return PolygonShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.MULTIPOINT -> return MultiPointPlainShape(
                    shapeHeader, shapeType, this.`is`,
                    this.rules
                )

                ShapeType.POINT_Z -> return PointZShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.POLYLINE_Z -> return PolylineZShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.POLYGON_Z -> return PolygonZShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.MULTIPOINT_Z -> return MultiPointZShape(shapeHeader, shapeType, this.`is`, this.rules)

                ShapeType.POINT_M -> return PointMShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.POLYLINE_M -> return PolylineMShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.POLYGON_M -> return PolygonMShape(shapeHeader, shapeType, this.`is`, this.rules)
                ShapeType.MULTIPOINT_M -> return MultiPointMShape(shapeHeader, shapeType, this.`is`, this.rules)

                ShapeType.MULTIPATCH -> return MultiPatchShape(shapeHeader, shapeType, this.`is`, this.rules)

                else -> throw InvalidShapeFileException(
                    ("Unexpected shape type '"
                            + shapeType + "'")
                )
            }
        } catch (e: EOFException) {
            throw InvalidShapeFileException(
                ("Unexpected end of stream. "
                        + "The data is too short for the last shape (" + shapeType
                        + ") that was being read.")
            )
        }
    }

    // Getters
    /**
     * Returns the shape's header.
     *
     * @return shape's header.
     */
    fun getHeader(): ShapeFileHeader {
        return header
    }
}

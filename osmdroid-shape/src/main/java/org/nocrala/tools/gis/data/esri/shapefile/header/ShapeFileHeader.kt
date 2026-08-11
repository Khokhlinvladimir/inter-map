package org.nocrala.tools.gis.data.esri.shapefile.header

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.EOFException
import java.io.InputStream

class ShapeFileHeader(
    `is`: InputStream,
    rules: ValidationPreferences
) {
    // Getters
    var fileCode: Int = 0
        private set

    val unused0: Int
    val unused1: Int
    val unused2: Int
    val unused3: Int
    val unused4: Int

    val fileLength: Int
    var version: Int = 0
        private set
    var shapeType: ShapeType? = null
        private set

    val boxMinX: Double
    val boxMinY: Double
    val boxMaxX: Double
    val boxMaxY: Double

    val boxMinZ: Double
    val boxMaxZ: Double

    val boxMinM: Double
    val boxMaxM: Double

    init {
        try {
            this.fileCode = ISUtil.readBeInt(`is`)
            if (this.fileCode != SHAPE_FILE_CODE) {
                throw InvalidShapeFileException(
                    ("Invalid shape file code. Found " + this.fileCode
                            + " but expected " + SHAPE_FILE_CODE + ".")
                )
            }

            this.unused0 = ISUtil.readBeInt(`is`)
            this.unused1 = ISUtil.readBeInt(`is`)
            this.unused2 = ISUtil.readBeInt(`is`)
            this.unused3 = ISUtil.readBeInt(`is`)
            this.unused4 = ISUtil.readBeInt(`is`)

            this.fileLength = ISUtil.readBeInt(`is`)
            this.version = ISUtil.readLeInt(`is`)
            if (this.version != SHAPE_FILE_VERSION) {
                throw InvalidShapeFileException(
                    ("Invalid shape file version. Found " + this.version
                            + " but expected " + SHAPE_FILE_VERSION + ".")
                )
            }

            val shapeTypeId = ISUtil.readLeInt(`is`)
            if (rules.forceShapeType == null) {
                this.shapeType = ShapeType.parse(shapeTypeId)
                if (this.shapeType == null) {
                    throw InvalidShapeFileException(
                        ("Invalid shape file. "
                                + "The header's shape type has the invalid code "
                                + shapeTypeId + ".")
                    )
                }
            } else {
                this.shapeType = rules.forceShapeType
            }

            this.boxMinX = ISUtil.readLeDouble(`is`)
            this.boxMinY = ISUtil.readLeDouble(`is`)
            this.boxMaxX = ISUtil.readLeDouble(`is`)
            this.boxMaxY = ISUtil.readLeDouble(`is`)

            this.boxMinZ = ISUtil.readLeDouble(`is`)
            this.boxMaxZ = ISUtil.readLeDouble(`is`)

            this.boxMinM = ISUtil.readLeDouble(`is`)
            this.boxMaxM = ISUtil.readLeDouble(`is`)
        } catch (e: EOFException) {
            throw InvalidShapeFileException(
                ("Unexpected end of stream. "
                        + "The content is too short. "
                        + "It doesn't even have a complete header.")
            )
        }
    }

    companion object {
        private const val SHAPE_FILE_CODE = 9994
        private const val SHAPE_FILE_VERSION = 1000
    }
}

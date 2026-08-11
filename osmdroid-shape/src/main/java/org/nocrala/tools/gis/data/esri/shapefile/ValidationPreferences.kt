package org.nocrala.tools.gis.data.esri.shapefile

import org.nocrala.tools.gis.data.esri.shapefile.shape.PartType
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType

/**
 * This class allows the client to relax the level of validation when reading a
 * shape file. When instantiated, all validations are turned on by default, so
 * the client can specifically turn off the required ones.
 */
class ValidationPreferences {
    var expectedRecordNumber: Int = 0
        private set

    /**
     * Forces the shape type to a specific type, disabling its validation. Set
     * this value to null to enable the validation. Defaults to null.
     *
     * @param forceShapeType
     */
    @JvmField
    var forceShapeType: ShapeType? = null

    /**
     * Inhibits the validation of the record numbers; a correct shape file must
     * have sequential record numbers, starting at 1. Defaults to false.
     *
     * @param this.isAllowBadRecordNumbers
     */
    var isAllowBadRecordNumbers: Boolean = false

    /**
     * Inhibits the validation of the content length of each shape. Defaults to
     * false.
     *
     * @param this.isAllowBadContentLength
     */
    var isAllowBadContentLength: Boolean = false

    /**
     * Allows shapes of multiple types in the file; in a correct shape file all
     * shapes must be of a single type, specified on the header of the file.
     * Defaults to false.
     *
     * @param this.isAllowMultipleShapeTypes
     */
    var isAllowMultipleShapeTypes: Boolean = false

    /**
     * Allows any (positive) number of points per shape. It's strongly advised to
     * always limit the number of points per shape; otherwise, a corrupt file with
     * a gigantic (garbage) number of points may crash the reader with an
     * OutOfMemory error. Defaults to false, with a default limit of 10000.
     *
     * @param this.isAllowUnlimitedNumberOfPointsPerShape
     */
    var isAllowUnlimitedNumberOfPointsPerShape: Boolean = false

    /**
     * Specifies the maximum number of points a shape can have. If a shape is
     * found with a larger number of points a exception is thrown showing the
     * number of points it has. This parameter can be adjusted for different
     * files, or turned off with the method
     * setAllowUnlimitedNumberOfPointsPerShape(). Defaults to 10000.
     *
     * @param maxItems
     */
    @JvmField
    var maxNumberOfPointsPerShape: Int = DEFAULT_MAX_NUMBER_OF_POINTS_PER_SHAPE

    /**
     * Forces the part types to a specific type, disabling its validation. Set
     * this value to null to enable the validation. Defaults to null.
     *
     * @param forcePartType
     */
    // Accessors
    var forcePartType: PartType? = null

    // Logic
    fun advanceOneRecordNumber() {
        this.expectedRecordNumber++
    }

    companion object {
        private const val DEFAULT_MAX_NUMBER_OF_POINTS_PER_SHAPE = 10000
    }
}

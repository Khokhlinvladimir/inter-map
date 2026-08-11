package org.nocrala.tools.gis.data.esri.shapefile.shape

import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException
import org.nocrala.tools.gis.data.esri.shapefile.util.ISUtil
import java.io.InputStream

class ShapeHeader(`is`: InputStream, rules: ValidationPreferences) {
    var recordNumber: Int
        private set
    @JvmField
    val contentLength: Int

    init {
        this.recordNumber = ISUtil.readBeIntMaybeEOF(`is`)
        if (!rules.isAllowBadRecordNumbers) {
            if (this.recordNumber != rules.expectedRecordNumber) {
                throw InvalidShapeFileException(
                    ("Invalid record number. Expected "
                            + rules.expectedRecordNumber + " but found "
                            + this.recordNumber + ".")
                )
            }
        }

        this.contentLength = ISUtil.readBeInt(`is`)
    }
}

package org.osmdroid.shape

import net.iryndin.jdbf.core.DbfRecord
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.text.ParseException

interface ShapeMetaSetter {
    @Throws(ParseException::class)
    fun set(metadata: DbfRecord?, marker: Marker)

    @Throws(ParseException::class)
    fun set(metadata: DbfRecord?, polygon: Polygon)

    @Throws(ParseException::class)
    fun set(metadata: DbfRecord?, polyline: Polyline)
}

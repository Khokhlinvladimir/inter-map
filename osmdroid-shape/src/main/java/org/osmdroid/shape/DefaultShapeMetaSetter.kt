package org.osmdroid.shape

import net.iryndin.jdbf.core.DbfRecord
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.nio.charset.Charset
import java.text.ParseException

class DefaultShapeMetaSetter : ShapeMetaSetter {
    @Throws(ParseException::class)
    override fun set(metadata: DbfRecord?, marker: Marker) {
        if (metadata != null) {
            metadata.setStringCharset(Charset.defaultCharset())
            marker.setSnippet(metadata.toMap().toString())
            marker.setTitle(getSensibleTitle(requireNotNull(marker.getSnippet())))
        }
    }

    @Throws(ParseException::class)
    override fun set(metadata: DbfRecord?, polygon: Polygon) {
        if (metadata != null) {
            metadata.setStringCharset(Charset.defaultCharset())
            polygon.setSnippet(metadata.toMap().toString())
            polygon.setTitle(getSensibleTitle(requireNotNull(polygon.getSnippet())))
        }
        val boundingBox = polygon.getBounds()
        polygon.setSubDescription(boundingBox.toString())
    }

    @Throws(ParseException::class)
    override fun set(metadata: DbfRecord?, polyline: Polyline) {
        if (metadata != null) {
            metadata.setStringCharset(Charset.defaultCharset())
            polyline.setSnippet(metadata.toMap().toString())
            polyline.setTitle(getSensibleTitle(requireNotNull(polyline.getSnippet())))
        }
    }

    companion object {
        private fun getSensibleTitle(snippet: String): String {
            if (snippet.length > 100) {
                return snippet.substring(0, 96) + "..."
            }
            return snippet
        }
    }
}

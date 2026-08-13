/**
 * The MIT License
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 * This code was sourced from the National Geospatial Intelligency Agency and was
 * originally licensed under the MIT license. It has been modified to support
 * osmdroid's APIs.
 *
 *
 * You can find the original code base here:
 * https://github.com/ngageoint/geopackage-android-map
 * https://github.com/ngageoint/geopackage-android
 */
package org.osmdroid.gpkg.overlay.features

import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.overlay.Marker


/**
 * Google Map Shape with markers
 *
 * @author osbornb
 */
class OsmdroidShapeMarkers {
    /**
     * Get the map shape
     *
     * @return map shape
     */
    /**
     * Set the map shape
     *
     * @param shape map shape
     */
    /**
     * Shape
     */
    var shape: OsmDroidMapShape? = null

    /**
     * Get the shape markers map
     *
     * @return shape markers map
     * @since 1.3.2
     */
    /**
     * Map between marker ids and shape markers they belong to (or null for non
     * shapes)
     */
    val shapeMarkersMap: MutableMap<String?, ShapeMarkers> = HashMap<String?, ShapeMarkers>()

    /**
     * Add the marker to the shape
     *
     * @param marker
     * @param shapeMarkers
     */
    /**
     * Add a marker with no shape
     *
     * @param marker
     */
    @JvmOverloads
    fun add(marker: Marker, shapeMarkers: ShapeMarkers? = null) {
        add(marker.getId(), shapeMarkers)
    }

    /**
     * Add the marker id to the shape
     *
     * @param markerId
     * @param shapeMarkers
     */
    fun add(markerId: String?, shapeMarkers: ShapeMarkers?) {
        shapeMarkersMap.put(markerId, shapeMarkers!!)
    }

    /**
     * Add all markers in the shape
     *
     * @param shapeMarkers
     */
    fun add(shapeMarkers: ShapeMarkers) {
        for (marker in shapeMarkers.getMarkers()) {
            add(marker, shapeMarkers)
        }
    }

    /**
     * Add a list of markers with no shape
     *
     * @param markers
     */
    fun add(markers: MutableList<Marker>) {
        for (marker in markers) {
            add(marker)
        }
    }

    /**
     * Add an embedded shape markers
     *
     * @param googleShapeMarkers
     */
    fun add(googleShapeMarkers: OsmdroidShapeMarkers) {
        shapeMarkersMap.putAll(googleShapeMarkers.shapeMarkersMap)
    }

    /**
     * Check if contains the marker
     *
     * @param marker
     * @return
     */
    fun contains(marker: Marker): Boolean {
        return contains(marker.getId())
    }

    /**
     * Check if contains the marker id
     *
     * @param markerId
     * @return
     */
    fun contains(markerId: String?): Boolean {
        return shapeMarkersMap.containsKey(markerId)
    }

    /**
     * Get the shape markers for a marker, only returns a value of shapes that
     * can be edited
     *
     * @param marker
     * @return
     */
    fun getShapeMarkers(marker: Marker): ShapeMarkers? {
        return getShapeMarkers(marker.getId())
    }

    /**
     * Get the shape markers for a marker id, only returns a value of shapes
     * that can be edited
     *
     * @param markerId
     * @return
     */
    fun getShapeMarkers(markerId: String?): ShapeMarkers? {
        return shapeMarkersMap.get(markerId)
    }

    val isValid: Boolean
        /**
         * Determines if the shape is in a valid state
         */
        get() {
            var valid = true
            if (shape != null) {
                valid = shape!!.isValid
            }
            return valid
        }

    /**
     * Updates visibility of all objects
     *
     * @param visible visible flag
     * @since 1.3.2
     */
    fun setVisible(visible: Boolean) {
        setVisibleMarkers(visible)
    }

    /**
     * Updates visibility of the shape representing markers
     *
     * @param visible visible flag
     * @since 1.3.2
     */
    fun setVisibleMarkers(visible: Boolean) {
        for (shapeMarkers in shapeMarkersMap.values) {
            shapeMarkers.setVisibleMarkers(visible)
        }
    }

    /**
     * Get the shape markers size
     *
     * @return size
     * @since 1.3.2
     */
    fun size(): Int {
        return shapeMarkersMap.size
    }

    val isEmpty: Boolean
        /**
         * Check if the shape markers is empty
         *
         * @return true if empty
         * @since 1.3.2
         */
        get() = shapeMarkersMap.isEmpty()

    companion object {
        /**
         * Polygon add a marker in the list of markers to where it is closest to the
         * the surrounding points
         *
         * @param marker
         * @param markers
         */
        fun addMarkerAsPolygon(marker: Marker, markers: MutableList<Marker>) {
            val position: IGeoPoint = marker.position
            var insertLocation = markers.size
            if (markers.size > 2) {
                val distances = DoubleArray(markers.size)
                insertLocation = 0
                distances[0] = SphericalUtil.computeDistanceBetween(
                    position,
                    markers[0].position
                )
                for (i in 1 until markers.size) {
                    distances[i] = SphericalUtil.computeDistanceBetween(
                        position,
                        markers[i].position
                    )
                    if (distances[i] < distances[insertLocation]) {
                        insertLocation = i
                    }
                }

                val beforeLocation = if (insertLocation > 0)
                    insertLocation - 1
                else
                    distances.size - 1
                val afterLocation = if (insertLocation < distances.size - 1)
                    insertLocation + 1
                else
                    0

                if (distances[beforeLocation] > distances[afterLocation]) {
                    insertLocation = afterLocation
                }
            }
            markers.add(insertLocation, marker)
        }

        /**
         * Polyline add a marker in the list of markers to where it is closest to
         * the the surrounding points
         *
         * @param marker
         * @param markers
         */
        fun addMarkerAsPolyline(marker: Marker, markers: MutableList<Marker>) {
            val position = marker.position
            var insertLocation = markers.size
            if (markers.size > 1) {
                val distances = DoubleArray(markers.size)
                insertLocation = 0
                distances[0] = SphericalUtil.computeDistanceBetween(
                    position,
                    markers[0].position
                )
                for (i in 1 until markers.size) {
                    distances[i] = SphericalUtil.computeDistanceBetween(
                        position,
                        markers[i].position
                    )
                    if (distances[i] < distances[insertLocation]) {
                        insertLocation = i
                    }
                }

                val beforeLocation = if (insertLocation > 0)
                    insertLocation - 1
                else
                    null
                val afterLocation = if (insertLocation < distances.size - 1)
                    insertLocation + 1
                else
                    null

                if (beforeLocation != null && afterLocation != null) {
                    if (distances[beforeLocation] > distances[afterLocation]) {
                        insertLocation = afterLocation
                    }
                } else if (beforeLocation != null) {
                    if (distances[beforeLocation] >= SphericalUtil
                            .computeDistanceBetween(
                                markers[beforeLocation].position,
                                markers[insertLocation].position
                            )
                    ) {
                        insertLocation++
                    }
                } else {
                    if (distances[afterLocation!!] < SphericalUtil
                            .computeDistanceBetween(
                                markers[afterLocation].position,
                                markers[insertLocation].position
                            )
                    ) {
                        insertLocation++
                    }
                }
            }
            markers.add(insertLocation, marker)
        }
    }
}

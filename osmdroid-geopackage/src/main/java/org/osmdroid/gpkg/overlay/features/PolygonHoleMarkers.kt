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

import org.osmdroid.views.overlay.Marker

/**
 * Polygon Hole with Markers object
 *
 * @author osbornb
 */
class PolygonHoleMarkers
/**
 * Constructor
 *
 * @param parentPolygon
 */(private val parentPolygon: PolygonMarkers?) : ShapeMarkers {
    private var markers: MutableList<Marker> = ArrayList<Marker>()

    fun add(marker: Marker?) {
        markers.add(marker!!)
    }

    /**
     * {@inheritDoc}
     */
    override fun getMarkers(): MutableList<Marker> {
        return markers
    }

    fun setMarkers(markers: MutableList<Marker>) {
        this.markers = markers
    }

    /**
     * Remove from the map
     *
     * public void remove() {
     * for (Marker marker : markers) {
     * marker.remove();
     * }
     * }  */
    /**
     * {@inheritDoc}
     */
    override fun setVisible(visible: Boolean) {
        setVisibleMarkers(visible)
    }

    /**
     * {@inheritDoc}
     */
    override fun setVisibleMarkers(visible: Boolean) {
        for (marker in markers) {
            marker.alpha = if (visible) 1f else 0f
        }
    }

    val isValid: Boolean
        /**
         * Is it valid
         *
         * @return
         */
        get() = markers.isEmpty() || markers.size >= 3

    val isDeleted: Boolean
        /**
         * Is it deleted
         *
         * @return
         */
        get() = markers.isEmpty()

    /**
     * {@inheritDoc}
     *
     * @Override public void delete(Marker marker) {
     * if (markers.remove(marker)) {
     * marker.remove();
     * parentPolygon.update();
     * }
     * }
     */
    /**
     * {@inheritDoc}
     */
    override fun addNew(marker: Marker?) {
        OsmdroidShapeMarkers.addMarkerAsPolygon(marker!!, markers)
    }
}

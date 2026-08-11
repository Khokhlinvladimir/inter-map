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

import org.osmdroid.gpkg.overlay.OsmMapShapeConverter
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Polygon with Markers object
 *
 * @author osbornb
 */
class PolygonMarkers
/**
 * Constructor
 *
 * @param converter
 */(private val converter: OsmMapShapeConverter?) : ShapeWithChildrenMarkers {
    var polygon: Polygon? = null

    private var markers: MutableList<Marker> = ArrayList<Marker>()

    var holes: MutableList<PolygonHoleMarkers> = ArrayList<PolygonHoleMarkers>()

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

    fun addHole(hole: PolygonHoleMarkers?) {
        holes.add(hole!!)
    }

    /**
     * Update based upon marker changes
     *
     * public void update() {
     * if (polygon != null) {
     * if (isDeleted()) {
     * remove();
     * } else {
     *
     * List<GeoPoint> points = converter.getPointsFromMarkers(markers);
     * polygon.setPoints(points);
     *
     * List<List></List><GeoPoint>> holePointList = new ArrayList<List></List><GeoPoint>>();
     * for (PolygonHoleMarkers hole : holes) {
     * if (!hole.isDeleted()) {
     * List<GeoPoint> holePoints = converter
     * .getPointsFromMarkers(hole.getMarkers());
     * holePointList.add(holePoints);
     * }
     * }
     * polygon.setHoles(holePointList);
     * }
     * }
     * }    </GeoPoint></GeoPoint></GeoPoint></GeoPoint> */
    /**
     * Remove from the map
     *
     * public void remove() {
     * if (polygon != null) {
     * polygon.remove();
     * polygon = null;
     * }
     * for (Marker marker : markers) {
     * marker.remove();
     * }
     * for (PolygonHoleMarkers hole : holes) {
     * hole.remove();
     * }
     * }     */
    /**
     * {@inheritDoc}
     */
    override fun setVisible(visible: Boolean) {
        if (polygon != null) {
            polygon!!.setVisible(visible)
        }
        for (marker in markers) {
            marker.setVisible(visible)
        }
        for (hole in holes) {
            hole.setVisible(visible)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun setVisibleMarkers(visible: Boolean) {
        for (marker in markers) {
            marker.setVisible(visible)
        }
        for (hole in holes) {
            hole.setVisibleMarkers(visible)
        }
    }

    val isValid: Boolean
        /**
         * Is it valid
         *
         * @return
         */
        get() {
            var valid = markers.isEmpty() || markers.size >= 3
            if (valid) {
                for (hole in holes) {
                    valid = hole.isValid
                    if (!valid) {
                        break
                    }
                }
            }
            return valid
        }

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
     * update();
     * }
     * }
     */
    /**
     * {@inheritDoc}
     */
    override fun addNew(marker: Marker?) {
        OsmdroidShapeMarkers.addMarkerAsPolygon(marker!!, markers)
    }

    /**
     * {@inheritDoc}
     */
    override fun createChild(): ShapeMarkers {
        val hole = PolygonHoleMarkers(this)
        holes.add(hole)
        return hole
    }
}

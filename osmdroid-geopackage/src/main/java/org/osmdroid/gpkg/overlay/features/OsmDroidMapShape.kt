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

import mil.nga.sf.GeometryType


/**
 * Google Map Shape
 *
 * @author osbornb
 */
class OsmDroidMapShape
/**
 * Constructor
 *
 * @param geometryType
 * @param shapeType
 * @param shape
 */(
    /**
     * Geometry type
     */
    var geometryType: GeometryType?,
    /**
     * Shape type
     */
    var shapeType: OsmMapShapeType,
    /**
     * Shape objects
     */
    var shape: Any
) {
    /**
     * Get the geometry type
     *
     * @return
     */
    /**
     * Set the geometry type
     *
     * @param geometryType
     */

    /**
     * Get the shape type
     *
     * @return
     */
    /**
     * Set the shape type
     *
     * @param shapeType
     */

    /**
     * Get the shape
     *
     * @return
     */
    /**
     * Set the shape
     *
     * @param shape
     */

    /**
     * Removes all objects added to the map
     *
     * public void remove() {
     *
     * switch (shapeType) {
     *
     * case MARKER:
     * ((Marker) shape).remove();
     * break;
     * case POLYGON:
     * ((Polygon) shape).remove();
     * break;
     * case POLYLINE:
     * ((Polyline) shape).remove();
     * break;
     * case MULTI_MARKER:
     * ((MultiMarker) shape).remove();
     * break;
     * case MULTI_POLYLINE:
     * ((MultiPolyline) shape).remove();
     * break;
     * case MULTI_POLYGON:
     * ((MultiPolygon) shape).remove();
     * break;
     * case POLYLINE_MARKERS:
     * ((PolylineMarkers) shape).remove();
     * break;
     * case POLYGON_MARKERS:
     * ((PolygonMarkers) shape).remove();
     * break;
     * case MULTI_POLYLINE_MARKERS:
     * ((MultiPolylineMarkers) shape).remove();
     * break;
     * case MULTI_POLYGON_MARKERS:
     * ((MultiPolygonMarkers) shape).remove();
     * break;
     * case COLLECTION:
     * @SuppressWarnings("unchecked") List<GoogleMapShape> shapeList = (List<GoogleMapShape>) shape;
     * for (GoogleMapShape shapeListItem : shapeList) {
     * shapeListItem.remove();
     * }
     * break;
     * default:
     * }
     *
     * }
    </GoogleMapShape></GoogleMapShape> */
    /**
     * Updates visibility of all objects
     *
     * @param visible visible flag
     * @since 1.3.2
     *
     * public void setVisible(boolean visible) {
     *
     * switch (shapeType) {
     *
     * case MARKER:
     * ((Marker) shape).setVisible(visible);
     * break;
     * case POLYGON:
     * ((Polygon) shape).setVisible(visible);
     * break;
     * case POLYLINE:
     * ((Polyline) shape).setVisible(visible);
     * break;
     * case MULTI_MARKER:
     * ((MultiMarker) shape).setVisible(visible);
     * break;
     * case MULTI_POLYLINE:
     * ((MultiPolyline) shape).setVisible(visible);
     * break;
     * case MULTI_POLYGON:
     * ((MultiPolygon) shape).setVisible(visible);
     * break;
     * case POLYLINE_MARKERS:
     * ((PolylineMarkers) shape).setVisible(visible);
     * break;
     * case POLYGON_MARKERS:
     * ((PolygonMarkers) shape).setVisible(visible);
     * break;
     * case MULTI_POLYLINE_MARKERS:
     * ((MultiPolylineMarkers) shape).setVisible(visible);
     * break;
     * case MULTI_POLYGON_MARKERS:
     * ((MultiPolygonMarkers) shape).setVisible(visible);
     * break;
     * case COLLECTION:
     * @SuppressWarnings("unchecked") List<GoogleMapShape> shapeList = (List<GoogleMapShape>) shape;
     * for (GoogleMapShape shapeListItem : shapeList) {
     * shapeListItem.setVisible(visible);
     * }
     * break;
     * default:
     * }
     *
     * }
    </GoogleMapShape></GoogleMapShape> */
    /**
     * Updates all objects that could have changed from moved markers
     *
     * public void update() {
     *
     * switch (shapeType) {
     *
     * case POLYLINE_MARKERS:
     * ((PolylineMarkers) shape).update();
     * break;
     * case POLYGON_MARKERS:
     * ((PolygonMarkers) shape).update();
     * break;
     * case MULTI_POLYLINE_MARKERS:
     * ((MultiPolylineMarkers) shape).update();
     * break;
     * case MULTI_POLYGON_MARKERS:
     * ((MultiPolygonMarkers) shape).update();
     * break;
     * case COLLECTION:
     * @SuppressWarnings("unchecked") List<GoogleMapShape> shapeList = (List<GoogleMapShape>) shape;
     * for (GoogleMapShape shapeListItem : shapeList) {
     * shapeListItem.update();
     * }
     * break;
     * default:
     * }
     *
     * }
    </GoogleMapShape></GoogleMapShape> */
    val isValid: Boolean
        /**
         * Determines if the shape is in a valid state
         */
        get() {
            var valid = true

            when (shapeType) {
                OsmMapShapeType.POLYLINE_MARKERS -> valid = (shape as PolylineMarkers).isValid
                OsmMapShapeType.POLYGON_MARKERS -> valid = (shape as PolygonMarkers).isValid
                OsmMapShapeType.MULTI_POLYLINE_MARKERS -> valid = (shape as MultiPolylineMarkers).isValid
                OsmMapShapeType.MULTI_POLYGON_MARKERS -> valid = (shape as MultiPolygonMarkers).isValid
                OsmMapShapeType.COLLECTION -> {
                    val shapeList = shape as MutableList<OsmDroidMapShape>
                    for (shapeListItem in shapeList) {
                        valid = shapeListItem.isValid
                        if (!valid) {
                            break
                        }
                    }
                }

                else -> {}
            }

            return valid
        }


    fun setVisible(visible: Boolean) {
    }
}

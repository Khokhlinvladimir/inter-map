package org.osmdroid.views.overlay.cluster

import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import java.util.LinkedHashMap
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** A value and its geographic position, ready for clustering. */
data class ClusterPoint<T>(val item: T, val position: IGeoPoint)

/** A cluster result. A single-item result represents an unclustered point. */
data class MapCluster<T>(val items: List<T>, val position: GeoPoint)

/**
 * Groups geographic points into a zoom-dependent Web Mercator grid.
 *
 * The grid is deterministic, independent of the viewport pixel origin, and supports points on
 * both sides of the antimeridian. Clustering is intentionally free of Android UI dependencies so
 * it can be used off the main thread and tested without a map view.
 */
class GridClusterAlgorithm @JvmOverloads constructor(
    var cellSizePixels: Int = DEFAULT_CELL_SIZE_PIXELS,
    var minimumClusterSize: Int = DEFAULT_MINIMUM_CLUSTER_SIZE,
    var maximumClusterZoom: Double = DEFAULT_MAXIMUM_CLUSTER_ZOOM
) {

    @JvmOverloads
    fun <T> cluster(
        points: Collection<ClusterPoint<T>>,
        zoomLevel: Double,
        referenceLongitude: Double = 0.0
    ): List<MapCluster<T>> {
        require(cellSizePixels > 0) { "cellSizePixels must be greater than zero" }
        require(minimumClusterSize > 1) { "minimumClusterSize must be greater than one" }
        require(zoomLevel.isFinite()) { "zoomLevel must be finite" }
        require(referenceLongitude.isFinite()) { "referenceLongitude must be finite" }
        if (points.isEmpty()) return emptyList()
        if (zoomLevel >= maximumClusterZoom) {
            return points.map { MapCluster(listOf(it.item), GeoPoint(it.position)) }
        }

        val zoom = max(0.0, zoomLevel)
        val worldSize = TILE_SIZE * 2.0.pow(zoom)
        val buckets = LinkedHashMap<GridCell, MutableList<ClusterPoint<T>>>()
        points.forEach { point ->
            require(point.position.latitude.isFinite() && point.position.longitude.isFinite()) {
                "Cluster coordinates must be finite"
            }
            val longitude = unwrapLongitude(point.position.longitude, referenceLongitude)
            val x = (longitude + 180.0) / 360.0 * worldSize
            val latitude = min(MAX_MERCATOR_LATITUDE, max(-MAX_MERCATOR_LATITUDE, point.position.latitude))
            val latitudeRadians = Math.toRadians(latitude)
            val y = (0.5 - ln((1.0 + sin(latitudeRadians)) / (1.0 - sin(latitudeRadians))) / (4.0 * PI)) * worldSize
            val cell = GridCell(floor(x / cellSizePixels).toLong(), floor(y / cellSizePixels).toLong())
            buckets.getOrPut(cell) { ArrayList() }.add(point)
        }

        return buildList {
            buckets.values.forEach { bucket ->
                if (bucket.size < minimumClusterSize) {
                    bucket.forEach { add(MapCluster(listOf(it.item), GeoPoint(it.position))) }
                } else {
                    add(MapCluster(bucket.map { it.item }, centroid(bucket)))
                }
            }
        }
    }

    private fun <T> centroid(points: List<ClusterPoint<T>>): GeoPoint {
        val latitude = points.sumOf { it.position.latitude } / points.size
        val longitudeX = points.sumOf { cos(Math.toRadians(it.position.longitude)) }
        val longitudeY = points.sumOf { sin(Math.toRadians(it.position.longitude)) }
        val longitude = Math.toDegrees(atan2(longitudeY, longitudeX))
        return GeoPoint(latitude, longitude)
    }

    private fun unwrapLongitude(longitude: Double, referenceLongitude: Double): Double {
        var result = longitude
        while (result - referenceLongitude > 180.0) result -= 360.0
        while (result - referenceLongitude < -180.0) result += 360.0
        return result
    }

    private data class GridCell(val x: Long, val y: Long)

    companion object {
        const val DEFAULT_CELL_SIZE_PIXELS = 96
        const val DEFAULT_MINIMUM_CLUSTER_SIZE = 2
        const val DEFAULT_MAXIMUM_CLUSTER_ZOOM = 18.0
        private const val TILE_SIZE = 256.0
        private const val MAX_MERCATOR_LATITUDE = 85.05112877980659
    }
}

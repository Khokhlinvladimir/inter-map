package org.osmdroid.views.overlay.cluster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.GeoPoint

class GridClusterAlgorithmTest {
    private val algorithm = GridClusterAlgorithm(cellSizePixels = 96, maximumClusterZoom = 18.0)

    @Test
    fun nearbyPointsShareACluster() {
        val result = algorithm.cluster(
            listOf(
                point("a", 55.7510, 37.6170),
                point("b", 55.7511, 37.6171),
                point("c", 55.7512, 37.6172)
            ),
            zoomLevel = 12.0,
            referenceLongitude = 37.0
        )

        assertEquals(1, result.size)
        assertEquals(listOf("a", "b", "c"), result.single().items)
    }

    @Test
    fun distantPointsRemainSeparate() {
        val result = algorithm.cluster(
            listOf(point("moscow", 55.7510, 37.6170), point("london", 51.5072, -0.1276)),
            zoomLevel = 8.0
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.items.size == 1 })
    }

    @Test
    fun maximumZoomDisablesClustering() {
        val points = listOf(point("a", 55.7510, 37.6170), point("b", 55.75101, 37.61701))

        val result = algorithm.cluster(points, zoomLevel = 18.0)

        assertEquals(2, result.size)
        assertTrue(result.all { it.items.size == 1 })
    }

    @Test
    fun antimeridianPointsCanShareACluster() {
        val result = algorithm.cluster(
            listOf(point("east", 0.0, 179.999), point("west", 0.0, -179.999)),
            zoomLevel = 10.0,
            referenceLongitude = 180.0
        )

        assertEquals(1, result.size)
        assertEquals(2, result.single().items.size)
        assertTrue(kotlin.math.abs(result.single().position.longitude) > 179.9)
    }

    @Test
    fun minimumClusterSizeLeavesSmallBucketsAsPoints() {
        algorithm.minimumClusterSize = 3
        val result = algorithm.cluster(
            listOf(point("a", 55.7510, 37.6170), point("b", 55.75101, 37.61701)),
            zoomLevel = 12.0,
            referenceLongitude = 37.0
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.items.size == 1 })
    }

    @Test
    fun fractionalZoomChangesTheEffectiveGridAtTheSameRateAsTheMap() {
        val points = listOf(point("a", 0.0, 0.0), point("b", 0.0, 0.05))

        val lowZoom = algorithm.cluster(points, zoomLevel = 10.0)
        val higherFractionalZoom = algorithm.cluster(points, zoomLevel = 10.9)

        assertEquals(1, lowZoom.size)
        assertEquals(2, higherFractionalZoom.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonFiniteCoordinatesAreRejected() {
        algorithm.cluster(listOf(point("bad", Double.NaN, 0.0)), zoomLevel = 10.0)
    }

    private fun point(id: String, latitude: Double, longitude: Double) =
        ClusterPoint(id, GeoPoint(latitude, longitude))
}

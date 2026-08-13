package org.osmdroid.util

import org.junit.Assert
import org.junit.Test
import org.osmdroid.api.IGeoPoint

class BoundBoxTest {
    private val tileSystem: TileSystem = TileSystemWebMercator()

    @Test
    fun testBoundingBox() {
        val points: MutableList<IGeoPoint> = mutableListOf(
            GeoPoint(1.0, 1.0),
            GeoPoint(1.0, -1.0),
            GeoPoint(-1.0, 1.0),
            GeoPoint(-1.0, -1.0),
            GeoPoint(0.0, 0.0)
        )
        val box = BoundingBox.fromGeoPoints(points)
        Assert.assertEquals(0.0, box.centerWithDateLine.latitude, 0.000001)
        Assert.assertEquals(0.0, box.centerWithDateLine.longitude, 0.000001)
        Assert.assertEquals(1.0, box.latNorth, 0.000001)
        Assert.assertEquals(-1.0, box.latSouth, 0.000001)
        Assert.assertEquals(1.0, box.lonEast, 0.000001)
        Assert.assertEquals(-1.0, box.lonWest, 0.000001)
    }

    @Test
    fun testBoundingBoxMax() {
        val points: MutableList<IGeoPoint> = mutableListOf(
            GeoPoint(tileSystem.maxLatitude, 180.0),
            GeoPoint(tileSystem.minLatitude, -180.0)
        )
        val box = BoundingBox.fromGeoPoints(points)
        Assert.assertEquals(0.0, box.centerWithDateLine.latitude, 0.000001)
        Assert.assertEquals(0.0, box.centerWithDateLine.longitude, 0.000001)
        Assert.assertEquals(tileSystem.maxLatitude, box.latNorth, 0.000001)
        Assert.assertEquals(tileSystem.minLatitude, box.latSouth, 0.000001)
        Assert.assertEquals(180.0, box.lonEast, 0.000001)
        Assert.assertEquals(-180.0, box.lonWest, 0.000001)
    }

    @Test
    fun testBoundingBoxAllNegs() {
        val box = BoundingBox.fromGeoPoints(mutableListOf(GeoPoint(-46.0, -46.0), GeoPoint(-45.0, -45.0)))
        Assert.assertEquals(-45.5, box.centerWithDateLine.latitude, 0.000001)
        Assert.assertEquals(-45.5, box.centerWithDateLine.longitude, 0.000001)
        Assert.assertEquals(-45.0, box.latNorth, 0.000001)
        Assert.assertEquals(-46.0, box.latSouth, 0.000001)
        Assert.assertEquals(-45.0, box.lonEast, 0.000001)
        Assert.assertEquals(-46.0, box.lonWest, 0.000001)
    }

    @Test
    fun testBoundingBoxIrregular() {
        val points = mutableListOf(
            GeoPoint(27.821134999999998, -97.21217899999999),
            GeoPoint(27.822409999999998, -97.211607),
            GeoPoint(27.835423, -97.20577),
            GeoPoint(27.837301, -97.204944),
            GeoPoint(27.837668999999998, -97.204782),
            GeoPoint(27.838047, -97.204616),
            GeoPoint(27.838178, -97.19545699999999),
            GeoPoint(27.838185, -97.194859),
            GeoPoint(27.838179, -97.19440399999999),
            GeoPoint(27.838168, -97.194245),
            GeoPoint(27.838165999999998, -97.194212),
            GeoPoint(27.838148999999998, -97.194105),
            GeoPoint(27.838144, -97.194086),
            GeoPoint(27.838071, -97.19375699999999),
            GeoPoint(27.838037999999997, -97.19363799999999),
            GeoPoint(27.838030999999997, -97.193619),
            GeoPoint(27.837996999999998, -97.193512),
            GeoPoint(27.837979999999998, -97.193468),
            GeoPoint(27.837951999999998, -97.19339699999999),
            GeoPoint(27.837901, -97.19326699999999),
            GeoPoint(27.837878999999997, -97.19318299999999),
            GeoPoint(27.83786, -97.193111),
            GeoPoint(27.837595, -97.19321),
            GeoPoint(27.836557, -97.19368999999999),
            GeoPoint(27.836017, -97.193941),
            GeoPoint(27.834646, -97.194563),
            GeoPoint(27.833799, -97.19493899999999),
            GeoPoint(27.832649999999997, -97.19543399999999),
            GeoPoint(27.832535, -97.195484),
            GeoPoint(27.832310999999997, -97.195588),
            GeoPoint(27.831644999999998, -97.195914),
            GeoPoint(27.831421, -97.196018),
            GeoPoint(27.831360999999998, -97.19604299999999),
            GeoPoint(27.831025999999998, -97.19621),
            GeoPoint(27.830997999999997, -97.196225),
            GeoPoint(27.830274, -97.196467),
            GeoPoint(27.829973, -97.196595),
            GeoPoint(27.829829999999998, -97.196657),
            GeoPoint(27.829731, -97.19667899999999),
            GeoPoint(27.829615999999998, -97.196699),
            GeoPoint(27.829829, -97.19760199999999),
            GeoPoint(27.829442999999998, -97.197783),
            GeoPoint(27.829482, -97.19800000000001)
        )
        val box = BoundingBox.fromGeoPoints(points)
        Assert.assertEquals(27.838185, box.latNorth, 0.00001)
        Assert.assertEquals(27.821134, box.latSouth, 0.00001)
        Assert.assertEquals(-97.193111, box.lonEast, 0.00001)
        Assert.assertEquals(-97.212178, box.lonWest, 0.00001)
    }
}

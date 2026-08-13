package org.osmdroid.util

import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Test
import org.osmdroid.views.MapView

class BoundingBoxTest {
    @Test
    fun testGetCenterLongitude() {
        assertEquals(1.5, BoundingBox.getCenterLongitude(1.0, 2.0), TOLERANCE)
        assertEquals(-178.5, BoundingBox.getCenterLongitude(2.0, 1.0), TOLERANCE)
    }

    @Test
    fun testOverlaps() {
        val box = BoundingBox(1.0, 1.0, -1.0, -1.0)
        Assert.assertTrue(box.overlaps(box, 4.0))
        var farAway = BoundingBox(45.0, 45.0, 44.0, 44.0)
        Assert.assertTrue(farAway.overlaps(farAway, 4.0))
        Assert.assertFalse(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(1.1, 45.0, 1.0, 44.0)
        Assert.assertTrue(farAway.overlaps(farAway, 4.0))
        Assert.assertFalse(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(2.0, 2.0, -2.0, -2.0)
        Assert.assertTrue(farAway.overlaps(farAway, 4.0))
        Assert.assertTrue(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(.5, .5, -.5, -.5)
        Assert.assertTrue(farAway.overlaps(farAway, 4.0))
        Assert.assertTrue(box.overlaps(farAway, 4.0))
    }

    @Test
    fun getSpansWithoutDateLine() {
        val box = BoundingBox(10.0, 10.0, -10.0, -10.0)
        assertEquals(20.0, box.longitudeSpanWithDateLine, TOLERANCE)
        assertEquals(20.0, box.longitudeSpan, TOLERANCE)
        assertEquals(20.0, box.latitudeSpan, TOLERANCE)
    }

    @Test
    fun testOverlapsWorld() {
        val tileSystem: TileSystem = TileSystemWebMercator()
        val box = BoundingBox(tileSystem.maxLatitude, 180.0, tileSystem.minLatitude, -180.0)
        Assert.assertTrue(box.overlaps(box, 4.0))
        var farAway = BoundingBox(45.0, 44.0, 44.0, 45.0)
        Assert.assertTrue(farAway.overlaps(farAway, 4.0))
        Assert.assertTrue(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(1.0, 44.0, 1.0, 45.0)
        Assert.assertTrue(farAway.overlaps(farAway, 4.0))
        Assert.assertTrue(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(2.0, 2.0, -2.0, -2.0)
        Assert.assertTrue(farAway.overlaps(farAway, 4.0))
        Assert.assertTrue(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(.5, .5, -.5, -.5)
        Assert.assertTrue(box.overlaps(farAway, 4.0))
    }

    @Test
    fun testOverlapsDateLine() {
        val box = BoundingBox(45.0, -178.0, -45.0, 178.0)
        Assert.assertTrue(box.overlaps(box, 4.0))
        var farAway = BoundingBox(45.0, 45.0, 44.0, 44.0)
        Assert.assertFalse(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(1.0, 45.0, 1.0, 44.0)
        Assert.assertFalse(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(2.0, 2.0, -2.0, -2.0)
        Assert.assertFalse(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(.5, .5, -.5, -.5)
        Assert.assertFalse(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(1.0, -179.0, -1.0, 179.0)
        Assert.assertTrue(box.overlaps(farAway, 4.0))
    }

    @Test
    fun testOverlapsDateLine2() {
        var box = BoundingBox(45.0, -178.0, -45.0, -1.0)
        Assert.assertTrue(box.overlaps(box, 4.0))
        var farAway = BoundingBox(45.0, -74.0, 44.0, -72.0)
        Assert.assertFalse(box.overlaps(farAway, 4.0))
        box = BoundingBox(45.0, 0.0, -45.0, 170.0)
        Assert.assertTrue(box.overlaps(box, 4.0))
        farAway = BoundingBox(40.0, -72.0, 38.0, -74.0)
        Assert.assertTrue(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(40.0, 5.0, 38.0, 4.0)
        Assert.assertFalse(box.overlaps(farAway, 4.0))
        farAway = BoundingBox(-40.0, 5.0, -42.0, 4.0)
        Assert.assertFalse(box.overlaps(farAway, 4.0))
    }

    @Test
    fun testOverlap2() {
        val box = BoundingBox(1.0, 1.0, -1.0, -1.0)
        Assert.assertTrue(box.overlaps(box, 4.0))
        var item = BoundingBox(2.0, 1.0, 1.0, -1.0)
        Assert.assertTrue(box.overlaps(item, 4.0))
        Assert.assertTrue(item.overlaps(box, 4.0))
        item = BoundingBox(1.0, -1.0, -1.0, -2.0)
        Assert.assertTrue(box.overlaps(item, 4.0))
        item = BoundingBox(1.0, 2.0, -1.0, 1.0)
        Assert.assertTrue(box.overlaps(item, 4.0))
        item = BoundingBox(-1.0, 1.0, -2.0, -1.0)
        Assert.assertTrue(box.overlaps(item, 4.0))
        item = BoundingBox(-2.0, 1.0, -4.0, -1.0)
        Assert.assertTrue(box.overlaps(item, 4.0))
    }

    @Test
    fun testSouthernBounds1() {
        BoundingBox(33.29456881383961, -105.6820678709375, 31.99535790385963, -106.67083740234375)
        BoundingBox(31.9277, -106.441352, 31.686508, -106.49126)
    }

    @Test
    fun testSouthernBoundsSimple() {
        val view = BoundingBox(2.0, 2.0, -2.0, -2.0)
        val item = BoundingBox(1.0, 1.0, 2.1, -1.0)
        Assert.assertTrue(view.overlaps(item, 4.0))
    }

    @Test
    fun testNorthernBoundsSimple() {
        val view = BoundingBox(2.0, 2.0, -2.0, -2.0)
        var item = BoundingBox(2.1, 2.0, 0.0, -2.0)
        Assert.assertTrue(view.overlaps(item, 4.0))
        item = BoundingBox(2.1, 2.0, 1.9, -2.0)
        Assert.assertTrue(view.overlaps(item, 4.0))
        item = BoundingBox(3.1, 2.0, 1.999999999, -2.0)
        Assert.assertTrue(view.overlaps(item, 4.0))
        item = BoundingBox(3.1, 2.0, 2.0, -2.0)
        Assert.assertTrue(view.overlaps(item, 4.0))
        item = BoundingBox(3.1, 2.0, 2.1, -2.0)
        Assert.assertFalse(view.overlaps(item, 4.0))
    }

    @Test
    fun testCorpusChristi() {
        val item = BoundingBox(27.696581, -97.243682999999, 27.688781, -97.253063)
        val view = BoundingBox(27.72243591897344, -97.24737167358398, 27.63730702015522, -97.30916976928711)
        Assert.assertTrue(view.overlaps(item, 4.0))
    }

    @Test
    fun testCorpusChristiViewIsNorth() {
        val item = BoundingBox(27.696581, -97.243682999999, 27.688781, -97.253063)
        val view = BoundingBox(27.782999124172314, -97.24748611450195, 27.697917493482727, -97.30928421020508)
        Assert.assertTrue(view.overlaps(item, 4.0))
    }

    @Test
    fun testDrawSetupLowZoom2() {
        val view = BoundingBox(83.17404, 142.74437, -18.14585, 7.73437)
        val drawing = BoundingBox(69.65708, 112.85162, 48.45835, 76.64063)
        Assert.assertTrue(view.overlaps(drawing, 4.0))
        val brokenView = BoundingBox(83.18311, -167.51953, -18.31281, 57.48046)
        Assert.assertTrue(brokenView.overlaps(drawing, 3.0))
    }

    @Test
    fun getSpansWithDateLine() {
        var box = BoundingBox(10.0, -170.0, -10.0, 170.0)
        assertEquals(20.0, box.longitudeSpanWithDateLine, TOLERANCE)
        assertEquals(20.0, box.latitudeSpan, TOLERANCE)
        box = BoundingBox(10.0, -10.0, -10.0, 10.0)
        assertEquals(340.0, box.longitudeSpanWithDateLine, TOLERANCE)
    }

    @Test
    fun increaseByScale() {
        val box = BoundingBox(10.0, 20.0, 0.0, 0.0).increaseByScale(1.2f)
        assertEquals(11.0, box.latNorth, TOLERANCE)
        assertEquals(22.0, box.lonEast, TOLERANCE)
        assertEquals(-1.0, box.latSouth, TOLERANCE)
        assertEquals(-2.0, box.lonWest, TOLERANCE)
    }

    @Test
    fun increaseByScale_onDateLine() {
        val box = BoundingBox(10.0, -170.0, -10.0, 170.0).increaseByScale(1.2f)
        assertEquals(12.0, box.latNorth, TOLERANCE)
        assertEquals(-168.0, box.lonEast, TOLERANCE)
        assertEquals(-12.0, box.latSouth, TOLERANCE)
        assertEquals(168.0, box.lonWest, TOLERANCE)
    }

    @Test
    fun increaseByScale_clipNorth() {
        val box = BoundingBox(80.0, 20.0, 0.0, -20.0).increaseByScale(1.2f)
        assertEquals(MapView.getTileSystem().maxLatitude, box.latNorth, TOLERANCE)
        assertEquals(-8.0, box.latSouth, TOLERANCE)
    }

    @Test
    fun increaseByScale_clipSouth() {
        val box = BoundingBox(0.0, 20.0, -80.0, -20.0).increaseByScale(1.2f)
        assertEquals(8.0, box.latNorth, TOLERANCE)
        assertEquals(MapView.getTileSystem().minLatitude, box.latSouth, TOLERANCE)
    }

    @Test
    fun increaseByScale_wrapEast() {
        val box = BoundingBox(20.0, 175.0, -20.0, 75.0).increaseByScale(1.2f)
        assertEquals(-175.0, box.lonEast, TOLERANCE)
        assertEquals(65.0, box.lonWest, TOLERANCE)
    }

    @Test
    fun increaseByScale_wrapWest() {
        val box = BoundingBox(20.0, -75.0, -20.0, -175.0).increaseByScale(1.2f)
        assertEquals(-65.0, box.lonEast, TOLERANCE)
        assertEquals(175.0, box.lonWest, TOLERANCE)
    }

    companion object {
        private const val TOLERANCE = 1E-5
    }
}

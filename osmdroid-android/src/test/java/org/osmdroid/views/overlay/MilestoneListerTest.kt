package org.osmdroid.views.overlay

import junit.framework.Assert
import org.junit.Test
import org.osmdroid.views.overlay.milestones.MilestoneLister

class MilestoneListerTest {
    @Test
    fun test_orientation() {
        Assert.assertEquals(0.0, MilestoneLister.getOrientation(1, 1, 1, 1), DELTA)
        Assert.assertEquals(0.0, MilestoneLister.getOrientation(1, 1, 10, 1), DELTA)
        Assert.assertEquals(45.0, MilestoneLister.getOrientation(10, 10, 20, 20), DELTA)
        Assert.assertEquals(90.0, MilestoneLister.getOrientation(10, 10, 10, 20), DELTA)
        Assert.assertEquals(180.0, MilestoneLister.getOrientation(10, 10, 0, 10), DELTA)
        Assert.assertEquals(-90.0, MilestoneLister.getOrientation(10, 10, 10, 0), DELTA)
    }

    companion object {
        private const val DELTA = 1E-10
    }
}

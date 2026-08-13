package org.osmdroid.bugtestfragments

import org.osmdroid.config.Configuration.instance
import org.osmdroid.samplefragments.BaseSampleFragment

/**
 * This test case will force the memory cache to be too small, effectively recreating bug
 * 846. See [#846](https://github.com/osmdroid/osmdroid/issues/846).
 * created on 1/15/2018.
 *
 * @author Alex O'Ree
 */
class Bug846InfiniteRedrawLoop : BaseSampleFragment() {
    init {
        instance!!.cacheMapTileCount = 0.toShort()
        instance!!.cacheMapTileOvershoot = (-3).toShort()
    }

    override val sampleTitle: String
        get() = "Infinite Redraw Loop"

    public override fun addOverlays() {
        super.addOverlays()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance!!.cacheMapTileCount = 9.toShort()
        instance!!.cacheMapTileOvershoot = 0.toShort()
    }
}

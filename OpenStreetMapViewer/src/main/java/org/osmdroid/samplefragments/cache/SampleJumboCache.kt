/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.osmdroid.samplefragments.cache

import android.os.Bundle
import org.osmdroid.config.Configuration.instance
import org.osmdroid.samplefragments.BaseSampleFragment

/**
 * An example on increasing the in memory tile cache. This is NOT the disk cache!
 *
 *
 * Caution, setting these values too high may cause OOM errors on less capable devices!
 *
 * @author alex
 */
class SampleJumboCache : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    init {
        instance!!.cacheMapTileCount = 12.toShort()
        instance!!.cacheMapTileOvershoot = 12.toShort()
    }

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * Called when the activity is first created.
     */
    public override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun addOverlays() {
        super.addOverlays()
    }

    public override fun onPause() {
        super.onPause()
        //reset the defaults
        instance!!.cacheMapTileCount = 9.toShort()
        instance!!.cacheMapTileOvershoot = 0.toShort()
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private const val SAMPLE_TITLE: String = "Jumbo Memory Cache"
    }
}

package org.osmdroid.bugtestfragments

import android.util.Log
import android.widget.Toast
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import kotlin.math.min

/**
 * Created by alex on 10/21/16.
 */
class Bug445Caching : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Bug 445 Ensure Caching works"

    var writer: SqlTileWriter? = null

    override fun addOverlays() {
        val tileWriter = mMapView!!.getTileProvider()!!.getTileWriter()

        if (tileWriter is SqlTileWriter) {
            writer = tileWriter
            writer!!.purgeCache()
        }
        setZoomAndCenter(initialZoom)
    }

    override fun skipOnCiTests(): Boolean {
        return true
    }

    @Throws(Exception::class)
    override fun runTestProcedures() {
        if (writer == null) return
        mMapView!!.setUseDataConnection(true)
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                Toast.makeText(getActivity(), "downloading from zoom level " + minZoom + " to " + maxZoom, Toast.LENGTH_SHORT).show()
                setZoomAndCenter(initialZoom)
            }
        })

        writer!!.purgeCache()
        val count = this.dbCount
        if (count != 0L) throw Exception("purge should remove all tiles, but " + count + " were found")

        var maxTilesNeeded = 0
        for (zoom in minZoom..maxZoom) {
            maxTilesNeeded += getMaxTileExpected(zoom)
        }
        mMapView!!.getTileProvider()!!.ensureCapacity(maxTilesNeeded)

        for (zoom in minZoom..maxZoom) {
            checkDownload(zoom)
        }

        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                Toast.makeText(getActivity(), "testing cache from zoom level " + minZoom + " to " + maxZoom, Toast.LENGTH_SHORT).show()
                setZoomAndCenter(initialZoom)
            }
        })

        mMapView!!.setUseDataConnection(false)

        for (zoom in minZoom..maxZoom) {
            checkCache(zoom)
        }

        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                Toast.makeText(getActivity(), "done", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * @since 6.0.0
     */
    @Throws(Exception::class)
    private fun checkDownload(pZoomLevel: Int) {
        val countBefore = this.dbCount
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                Toast.makeText(getActivity(), "checking download for zoom level " + pZoomLevel, Toast.LENGTH_SHORT).show()
                setZoomAndCenter(pZoomLevel)
            }
        })
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val countAfter = this.dbCount
        val count = countAfter - countBefore
        val minExpected = getMinTileExpected(pZoomLevel)
        if (count < minExpected) {
            throw Exception(
                ("only fetched " + count + " tiles"
                        + " for zoom level " + pZoomLevel
                        + " but " + minExpected + " were expected")
            )
        }
        Log.i(TAG, "checkDownload ok for zoom level " + pZoomLevel)
    }

    /**
     * @since 6.0.0
     */
    @Throws(Exception::class)
    private fun checkCache(pZoomLevel: Int) {
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                setZoomAndCenter(pZoomLevel)
            }
        })
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val queueSize = mMapView!!.getTileProvider()!!.getQueueSize()
        if (queueSize > 0) {
            throw Exception(
                ("queue size is greater than expected: " + queueSize
                        + " for zoom level " + pZoomLevel)
            )
        }
        Log.i(TAG, "checkCache ok for zoom level " + pZoomLevel)
    }

    /**
     * @since 6.0.0
     */
    private fun getMinTileExpected(pZoomLevel: Int): Int {
        val maxPerZoom = 1 shl pZoomLevel
        Log.i(TAG, "max per zoom " + maxPerZoom)
        val width = mMapView!!.getWidth()
        Log.i(TAG, "width " + width)
        val height = mMapView!!.getHeight()
        Log.i(TAG, "height " + height)
        val tileSize = TileSystem.tileSize
        Log.i(TAG, "tile size " + tileSize)
        val minCols = getMinNumberExpected(tileSize, width, maxPerZoom)
        Log.i(TAG, "min cols " + minCols)
        val minRows = getMinNumberExpected(tileSize, height, maxPerZoom)
        Log.i(TAG, "min rows " + minRows)
        val minExpected = minCols * minRows
        Log.i(TAG, "min expected " + minExpected)
        return minExpected
    }

    /**
     * @since 6.0.0
     */
    private fun getMaxTileExpected(pZoomLevel: Int): Int {
        val maxPerZoom = 1 shl pZoomLevel
        val width = mMapView!!.getWidth()
        val height = mMapView!!.getHeight()
        val tileSize = TileSystem.tileSize
        val minCols = getMaxNumberExpected(tileSize, width, maxPerZoom)
        val minRows = getMaxNumberExpected(tileSize, height, maxPerZoom)
        return minCols * minRows
    }

    /**
     * @since 6.0.0
     */
    private fun getMinNumberExpected(pTileSize: Int, pScreenSize: Int, pMaxPerZoom: Int): Int {
        return min(pMaxPerZoom, pScreenSize / pTileSize + (if (pScreenSize % pTileSize == 0) 0 else 1))
    }

    /**
     * @since 6.0.0
     */
    private fun getMaxNumberExpected(pTileSize: Int, pScreenSize: Int, pMaxPerZoom: Int): Int {
        return min(pMaxPerZoom, 1 + getMinNumberExpected(pTileSize, pScreenSize, pMaxPerZoom))
    }

    private val dbCount: Long
        /**
         * @since 6.0.0
         */
        get() {
            val count = writer!!.getRowCount(mMapView!!.getTileProvider()!!.getTileSource()!!.name())
            Log.i(TAG, "downloaded " + count + " tiles so far")
            return count
        }

    /**
     * @since 6.0.0
     */
    private fun setZoomAndCenter(pZoomLevel: Int) {
        mMapView!!.controller!!.setZoom(pZoomLevel)
        mMapView!!.controller!!.setCenter(center)
        mMapView!!.invalidate()
    }

    companion object {
        private val center = GeoPoint(52.2742, 0.21130)
        private const val minZoom = 10 // should be high enough so that download is needed (cf. archive)
        private const val maxZoom =
            16 // should be 16 or lower due to osm tile server policy (there is no systematic cache of tiles on server for zoom 17+)
        private val initialZoom: Int = minZoom - 1
    }
}

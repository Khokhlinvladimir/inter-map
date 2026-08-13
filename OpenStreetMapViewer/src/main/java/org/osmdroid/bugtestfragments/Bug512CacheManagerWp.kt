package org.osmdroid.bugtestfragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import org.osmdroid.R
import org.osmdroid.api.IMapView
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.cachemanager.CacheManager.CacheManagerCallback
import org.osmdroid.tileprovider.cachemanager.CacheManager.CacheManagerTask
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * https://github.com/osmdroid/osmdroid/issues/512#issuecomment-271219842
 * created on 1/8/2017.
 *
 * @author Alex O'Ree
 */
class Bug512CacheManagerWp : BaseSampleFragment(), CacheManagerCallback, View.OnClickListener {
    var btnCache: Button? = null

    override val sampleTitle: String
        get() = "Issue 512 Cache download using waypoints"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache!!.setOnClickListener(this)
        btnCache!!.setText("Run job (watch logcat output)")


        val onlineTileSourceBase = TileSourceFactory.USGS_SAT

        mMapView = MapView(
            getActivity()!!, MapTileProviderBasic(
                getActivity()!!.getApplicationContext(), onlineTileSourceBase
            )
        )

        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)

        return root
    }

    var downloadingTask: CacheManagerTask? = null

    override fun skipOnCiTests(): Boolean {
        return false
    }

    @Throws(Exception::class)
    override fun runTestProcedures() {
        val mgr = CacheManager(mMapView!!)
        val pts = ArrayList<GeoPoint>()
        pts.add(GeoPoint(38.89775, -77.03690))
        pts.add(GeoPoint(38.87101, -77.05641))
        taskRunning = true

        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                downloadingTask = mgr.downloadAreaAsyncNoUI(mMapView!!.getContext(), pts, 1, 4, this@Bug512CacheManagerWp)
            }
        })
        //downloadingTask = mgr.downloadAreaAsync(mMapView.getContext(), pts, 0, 5, this);
        var timeoutSeconds = 30
        while (taskRunning && timeoutSeconds > 0) {
            Thread.sleep(1000)
            timeoutSeconds--
        }
        if (!taskRunning) {
            //great we're done
            if (success) {
                //test passed
                return
            } else {
                throw RuntimeException("Failure occurred during the test, there were " + errors)
            }
        }
    }

    var taskRunning: Boolean = false
    var success: Boolean = false
    var errors: Int = 0

    override fun onTaskComplete() {
        Log.i(IMapView.LOGTAG, "download job complete no errors")
        taskRunning = false
        success = true
    }

    override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
        Log.i(IMapView.LOGTAG, "download update : " + progress + " " + currentZoomLevel + " " + zoomMin + " " + zoomMax)
    }

    override fun downloadStarted() {
        Log.i(IMapView.LOGTAG, "download job started")
    }

    override fun setPossibleTilesInArea(total: Int) {
        Log.i(IMapView.LOGTAG, "tiles to download " + total)
    }

    override fun onTaskFailed(errors: Int) {
        this.errors = errors
        Log.i(IMapView.LOGTAG, "down job failed with error count: " + errors)
        taskRunning = false
    }

    override fun onClick(v: View?) {
        try {
            runTestProcedures()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

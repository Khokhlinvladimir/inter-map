package org.osmdroid.samplefragments.data

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import org.osmdroid.R
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.OnFirstLayoutListener
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.IconOverlay
import org.osmdroid.views.overlay.Overlay
import kotlin.math.abs

/**
 * #394 #398 Demonstration how to load/update markers from
 * Async Background task.
 *
 *
 * Created by k3b on 01.09.2016.
 */
class AsyncTaskDemoFragment : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    override fun addOverlays() {
        super.addOverlays()

        mMapView!!.setTileSource(TileSourceFactory.MAPNIK)

        // If there is more than 200 millisecs no zoom/scroll update markers
        mMapView!!.setMapListener(DelayedMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                reloadMarker()
                return false
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                reloadMarker()
                return false
            }
        }, DEFAULT_INACTIVITY_DELAY_IN_MILLISECS.toLong()))

        mMapView!!.setMultiTouchControls(true)
        mMapView!!.setTilesScaledToDpi(true)

        val context: Context? = getActivity()
        mMarkerIcon = context!!.getResources().getDrawable(R.drawable.person)
        mCurrentBackgroundContentFolder = FolderOverlay()

        mMapView!!.getOverlays()!!.add(mCurrentBackgroundContentFolder)

        setHasOptionsMenu(true)

        // MapView.OnFirstLayoutListener initial map display also triggers onScroll to update the markers
        mMapView!!.addOnFirstLayoutListener(object : OnFirstLayoutListener {
            override fun onFirstLayout(v: View?, left: Int, top: Int, right: Int, bottom: Int) {
                mMapView!!.zoomToBoundingBox(BoundingBox(56.0, 7.0, 45.0, 16.0), false)
            }
        })
    }


    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Put overlay items first
        mMapView!!.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView)

        menu.add(0, MENU_ZOOMIN_ID, Menu.NONE, "ZoomIn")
        menu.add(0, MENU_ZOOMOUT_ID, Menu.NONE, "ZoomOut")

        super.onCreateOptionsMenu(menu, inflater)
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        mMapView!!.getOverlayManager().onPrepareOptionsMenu(menu, MENU_LAST_ID, mMapView)
        super.onPrepareOptionsMenu(menu)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMapView!!.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView)) {
            return true
        }

        when (item.getItemId()) {
            MENU_ZOOMIN_ID -> {
                mMapView!!.controller!!.zoomIn()
                return true
            }

            MENU_ZOOMOUT_ID -> {
                mMapView!!.controller!!.zoomOut()
                return true
            }
        }
        return false
    }

    //---------------------------------------------------------------
    /**
     * Load [FolderOverlay] with [IconOverlay]s in a Background Task [BackgroundMarkerLoaderTask].
     * mCurrentBackgroundMarkerLoaderTask.cancel() allows aboarding the loading task on screen rotation.
     * There are 0 or one tasks running at a time.
     */
    private var mCurrentBackgroundMarkerLoaderTask: BackgroundMarkerLoaderTask? = null

    /**
     * implementation detail: mMarkerIcon attached to each generated [IconOverlay]
     */
    private var mMarkerIcon: Drawable? = null

    /**
     * This must be reomoved from [org.osmdroid.views.MapView] when
     * [BackgroundMarkerLoaderTask] finishes
     */
    private var mCurrentBackgroundContentFolder: FolderOverlay? = null

    /**
     * if > 0 there where zoom/scroll events while [BackgroundMarkerLoaderTask] was active so
     * [.reloadMarker] bust be called again.
     */
    private var mMissedMapZoomScrollUpdates = 0

    /**
     * called by [org.osmdroid.views.MapView] if zoom or scroll has changed to
     * reload marker for new visible region in the [org.osmdroid.views.MapView]
     */
    private fun reloadMarker() {
        // initialized
        if (mCurrentBackgroundMarkerLoaderTask == null) {
            // start background load
            val zoom = this.mMapView!!.zoomLevelDouble
            val world = this.mMapView!!.getBoundingBox()

            reloadMarker(world!!, zoom)
        } else {
            // background load is already active. Remember that at least one scroll/zoom was missing
            mMissedMapZoomScrollUpdates++
        }
    }

    /**
     * called by MapView if zoom or scroll has changed to reload marker for new visible region
     */
    private fun reloadMarker(latLonArea: BoundingBox, zoom: Double) {
        Log.d(TAG, "reloadMarker " + latLonArea + ", zoom " + zoom)
        this.mCurrentBackgroundMarkerLoaderTask = BackgroundMarkerLoaderTask()
        this.mCurrentBackgroundMarkerLoaderTask!!.execute(
            latLonArea.getLatSouth(), latLonArea.getLatNorth(),
            latLonArea.getLonEast(), latLonArea.getLonWest(), zoom
        )
    }

    /**
     * Implements load [FolderOverlay] with [IconOverlay]s in a Background Task.
     */
    private inner class BackgroundMarkerLoaderTask : AsyncTask<Double?, Int?, FolderOverlay?>() {
        /**
         * Computation of the map itmes in the non-gui background thread. .
         *
         * @param params latMin, latMax, lonMin, longMax, zoom.
         * @return A new FolderOverlay that contain map data for latMin, latMax, lonMin, longMax, zoom.
         * @see .onPreExecute
         * @see .onPostExecute
         *
         * @see .publishProgress
         */
        override fun doInBackground(vararg params: Double?): FolderOverlay? {
            val result = FolderOverlay()

            try {
                require(params.size == 5) { "expected latMin, latMax, lonMin, longMax, zoom" }

                var paramNo = 0
                var latMin: Double = params[paramNo++]!!
                var latMax: Double = params[paramNo++]!!
                var lonMin: Double = params[paramNo++]!!
                var lonMax: Double = params[paramNo++]!!

                if (latMin > latMax) {
                    val t = latMax
                    latMax = latMin
                    latMin = t
                }
                if (latMax - latMin < 0.00001) return null

                //this is a problem, abort https://github.com/osmdroid/osmdroid/issues/521
                if (lonMin > lonMax) {
                    val t = lonMax
                    lonMax = lonMin
                    lonMin = t
                }
                val zoom = params[paramNo++]!!.toInt()

                Log.d(
                    TAG, "async doInBackground" +
                            " latMin=" + latMin +
                            " ,latMax=" + latMax +
                            " ,lonMin=" + lonMin +
                            " ,lonMax=" + lonMax +
                            ", zoom=" + zoom
                )
                // simulate heavy computation ...
                if (isCancelled()) return null
                Thread.sleep(1000, 0)
                if (isCancelled()) return null

                // i.e.
                // SELECT poi.lat, poi.lon, poi.id, poi.name FROM poi
                //    WHERE poi.lat >= {latMin} AND poi.lat <= {latMax}
                //          AND poi.lon >= {lonMin} AND poi.lon <= {lonMax}
                //          AND {zoom} >= poi.zoomMin AND {zoom} <= poi.zoomMax
                val latStep = abs(latMax - latMin) / 6
                val lonStep = abs(lonMax - lonMin) / 6
                var lat = latMin
                while (lat <= latMax) {
                    var lon = lonMin
                    while (lon <= lonMax) {
                        result.add(createMarker(lat, lon, zoom))
                        if (isCancelled()) break
                        lon += lonStep
                    }
                    if (isCancelled()) break

                    lat += latStep
                }
            } catch (ex: Exception) {
                // TODO more specific error handling
                Log.e(TAG, "doInBackground  " + ex.message, ex)
                cancel(false)
            }

            if (!isCancelled()) {
                Log.d(TAG, "doInBackground result " + result.getItems().size)
                return result
            }
            Log.d(TAG, "doInBackground cancelled")
            return null
        }

        // This is called in gui-thread when doInBackground() is finished.
        override fun onPostExecute(result: FolderOverlay?) {
            if (!isCancelled() && (result != null)) {
                showMarker(result)
            }
            mCurrentBackgroundMarkerLoaderTask = null
            // there was map move/zoom while {@link BackgroundMarkerLoaderTask} was active. must reload
            if (mMissedMapZoomScrollUpdates > 0) {
                Log.d(TAG, "onPostExecute: lost  " + mMissedMapZoomScrollUpdates + " MapZoomScrollUpdates. Reload items.")
                mMissedMapZoomScrollUpdates = 0
                reloadMarker()
            }
        }
    }

    private fun createMarker(lat: Double, lon: Double, zoom: Int): Overlay {
        return IconOverlay(GeoPoint(lat, lon), mMarkerIcon)
    }

    /**
     * called in gui thread by [BackgroundMarkerLoaderTask] after loading has finished.
     */
    private fun showMarker(newMarker: FolderOverlay?) {
        var modified = false
        if (this.mCurrentBackgroundContentFolder != null) {
            Log.d(TAG, "showMarker remove old " + this.mCurrentBackgroundContentFolder!!.getItems().size)
            this.mMapView!!.getOverlays()!!.remove(this.mCurrentBackgroundContentFolder)
            this.mCurrentBackgroundContentFolder!!.onDetach(mMapView)
            this.mCurrentBackgroundContentFolder = null
            modified = true
        }

        if (newMarker != null) {
            this.mCurrentBackgroundContentFolder = newMarker
            Log.d(TAG, "showMarker add new " + this.mCurrentBackgroundContentFolder!!.getItems().size + ", isAnimating=" + mMapView!!.isAnimating())
            mMapView!!.getOverlays()!!.add(newMarker)
            modified = true
        }

        if (modified) {
            if (mMapView!!.isAnimating()) {
                mMapView!!.postInvalidate()
            } else {
                mMapView!!.invalidate()
            }
        }
    }

    public override fun onDestroyView() {
        // called i.e. for screen rotation
        super.onDestroyView()
        if (mCurrentBackgroundMarkerLoaderTask != null) {
            // make shure that running {@link BackgroundMarkerLoaderTask} does not try to
            // update destroyed gui when finished
            mCurrentBackgroundMarkerLoaderTask!!.cancel(false)
            mCurrentBackgroundMarkerLoaderTask = null
        }
        super.onDestroy()
    }

    companion object {
        const val TAG: String = "osmAsync"

        /**
         * If there is more than 200 millisecs no zoom/scroll update markers
         */
        protected const val DEFAULT_INACTIVITY_DELAY_IN_MILLISECS: Int = 200

        // ===========================================================
        // Constants
        // ===========================================================
        private const val SAMPLE_TITLE: String = "AsyncTaskDemoFragment - Load Icons in AsyncTask"

        private val MENU_ZOOMIN_ID = Menu.FIRST
        private val MENU_ZOOMOUT_ID: Int = MENU_ZOOMIN_ID + 1
        private val MENU_LAST_ID: Int = MENU_ZOOMIN_ID + 1 // Always set to last unused id
    }
}

package org.osmdroid.samplefragments.events

import android.widget.Toast
import org.osmdroid.R
import org.osmdroid.data.DataRegion
import org.osmdroid.data.DataRegionLoader
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ScaleBarOverlay
import java.util.Timer
import java.util.TimerTask

/**
 * https://github.com/osmdroid/osmdroid/issues/264
 * extends gridlines to provide visual confirmation
 * Created by alex on 2/22/16.
 */
class SampleAnimateTo : SampleMapEventListener() {
    private var mIndex = 0
    private var mScaleBarOverlay: ScaleBarOverlay? = null
    private var t: Timer? = Timer()
    private var alive = true
    private val mList: MutableList<DataRegion> = ArrayList<DataRegion>()

    override val sampleTitle: String
        get() = "Animate To"


    public override fun addOverlays() {
        super.addOverlays()

        val dm = getActivity()!!.getResources().getDisplayMetrics()
        mScaleBarOverlay = ScaleBarOverlay(mMapView!!)
        mScaleBarOverlay!!.setCentred(true)
        mScaleBarOverlay!!.setScaleBarOffset(dm.widthPixels / 2, 10)
        mMapView!!.getOverlays()!!.add(mScaleBarOverlay)

        // according to https://www.flickr.com/places/info/12589342
        val manhattanCenter = GeoPoint(40.7909, -73.9664)
        val manhattanBoundingBox = BoundingBox(40.8820, -73.9067, 40.6829, -74.0479)
        // testing a "single point bounding box" (actually using zoom fallback in animateTo)
        mList.add(
            DataRegion(
                "dummy1", "Manhattan - single point",
                BoundingBox(manhattanCenter.latitude, manhattanCenter.longitude, manhattanCenter.latitude, manhattanCenter.longitude)
            )
        )
        // testing a "single latitude bounding box"
        mList.add(
            DataRegion(
                "dummy2", "Manhattan - single latitude",
                BoundingBox(manhattanCenter.latitude, manhattanBoundingBox.lonEast, manhattanCenter.latitude, manhattanBoundingBox.lonWest)
            )
        )
        // testing a "single longitude bounding box"
        mList.add(
            DataRegion(
                "dummy3", "Manhattan - single longitude",
                BoundingBox(
                    manhattanBoundingBox.latNorth,
                    manhattanCenter.longitude,
                    manhattanBoundingBox.latSouth,
                    manhattanCenter.longitude
                )
            )
        )
        // testing a "single longitude bounding box"
        mList.add(DataRegion("dummy4", "Manhattan - box", manhattanBoundingBox))

        try {
            mList.addAll(DataRegionLoader(getActivity()!!, R.raw.data_region_usstates).list.values.filterNotNull())
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }

        mMapView!!.post(object : Runnable {
            override fun run() {
                show()
            }
        })
    }

    public override fun onResume() {
        super.onResume()
        alive = true
        //some explanation here.
        //we using a timer task with a delayed start up to move the map around. during CI tests
        //this fragment can crash the app if you navigate away from the fragment before the initial fire
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                runTask()
            }
        }

        t = Timer()
        t!!.schedule(task, 4000, 4000)
    }

    public override fun onPause() {
        super.onPause()
        alive = false
        if (t != null) t!!.cancel()
        t = null
    }


    private fun runTask() {
        if (!alive) return
        if (getActivity() == null) {
            return
        }
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                if (mMapView == null || getActivity() == null) {
                    return
                }
                show()
            }
        })
    }

    /**
     * @since 6.0.2
     */
    public override fun onDestroyView() {
        super.onDestroyView()
        mScaleBarOverlay = null
    }

    /**
     * @since 6.0.2
     */
    /**
     * @since 6.0.2
     */
    private fun show(pIndex: Int = mIndex++) {
        val borderSizeInPixels = 20
        val zoomFallback = 12.0
        val animationSpeed: Long = 2000
        val animated = true
        val state = mList.get(pIndex % mList.size)
        val box = state.box
        mMapView!!.zoomToBoundingBox(box!!, animated, borderSizeInPixels, zoomFallback, animationSpeed)
        Toast.makeText(getActivity(), state.name, Toast.LENGTH_SHORT).show()
    }
}

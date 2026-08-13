package org.osmdroid.bugtestfragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Created by alex on 9/25/16.
 */
class Bug419Zoom : BaseSampleFragment(), View.OnClickListener {
    override val sampleTitle: String
        get() = "Zoom scaling calculations"

    var btnCache: Button? = null
    var executeJob: Button? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        mMapView = MapView(getActivity()!!)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache!!.setOnClickListener(this)
        btnCache!!.setText("Zoom Test")
        return root
    }

    override fun onClick(v: View) {
        if (v.getId() == R.id.btnCache) {
            //center as tne middle of the us to get a visual reference point
            mMapView!!.controller!!.setCenter(GeoPoint(38.73, -99.66))
            Thread(object : Runnable {
                override fun run() {
                    startTest()
                }
            }).start()
        }
    }

    var i: Double = 0.0
    var x: Double = 0.0

    //call this from off the UI thread
    fun startTest() {
        try {
            i = mMapView!!.getMinZoomLevel()
            while (i < mMapView!!.maxZoomLevel) {
                x = mMapView!!.maxZoomLevel
                while (x > mMapView!!.getMinZoomLevel()) {
                    Log.i(TAG, "Zoom out test " + i + " to " + x)
                    getActivity()!!.runOnUiThread(object : Runnable {
                        override fun run() {
                            mMapView!!.controller!!.setZoom(i)
                            mMapView!!.invalidate()
                        }
                    })
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    getActivity()!!.runOnUiThread(object : Runnable {
                        override fun run() {
                            mMapView!!.controller!!.zoomTo(x)
                            //mMapView.invalidate();
                        }
                    })
                    try {
                        //to let the tiles load
                        Thread.sleep(3000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    x--
                }
                i++
            }

            i = mMapView!!.maxZoomLevel
            while (i > mMapView!!.getMinZoomLevel()) {
                x = mMapView!!.getMinZoomLevel()
                while (x < mMapView!!.maxZoomLevel) {
                    Log.i(TAG, "Zoom out test " + i + " to " + x)
                    getActivity()!!.runOnUiThread(object : Runnable {
                        override fun run() {
                            mMapView!!.controller!!.setZoom(i)
                            mMapView!!.invalidate()
                        }
                    })
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    getActivity()!!.runOnUiThread(object : Runnable {
                        override fun run() {
                            mMapView!!.controller!!.zoomTo(x)
                            //mMapView.invalidate();
                        }
                    })
                    try {
                        //to let the tiles load
                        Thread.sleep(3000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    x++
                }
                i--
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}

package org.osmdroid.samplefragments.data

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.util.Log
import org.json.JSONObject
import org.osmdroid.R
import org.osmdroid.samplefragments.data.utils.JSONParser
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Timer
import java.util.TimerTask

/**
 * created on 1/7/2017.
 *
 * @author Alex O'Ree
 */
abstract class IISTrackerBase : SampleGridlines() {
    var alive: Boolean = true
    var marker: Marker? = null
    var sdf: SimpleDateFormat = SimpleDateFormat("HH:mm:ss.SSS yyyy-MMM-dd")

    var json: JSONParser = JSONParser()
    var nf: NumberFormat = DecimalFormat("###.#####")
    var cm: ConnectivityManager? = null

    abstract val isMotionTrail: Boolean

    var added: Boolean = false
    var motionTrailCounter: Int = 0
    var t: Timer? = null
    var task: TimerTask? = null
    var icon: Drawable? = null

    //Drawable icon_old;
    var image: Drawable? = null

    override fun addOverlays() {
        super.addOverlays()

        mMapView!!.setTilesScaledToDpi(true)
        mMapView!!.controller!!.setZoom(3)

        cm = getActivity()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        image = getResources().getDrawable(R.drawable.sfppt)
        icon = getResources().getDrawable(R.drawable.sfppt_small)

        //icon_old=getResources().getDrawable(R.drawable.sfppt_small);
        //icon_old.setAlpha(77);
        marker = Marker(mMapView)
        marker!!.setImage(image)
        marker!!.setIcon(icon)
        marker!!.setTitle("International Space Station")
    }

    public override fun onResume() {
        super.onResume()
        startTask()
    }

    private fun startTask() {
        task = object : TimerTask() {
            override fun run() {
                val location: GeoPoint? = this@IISTrackerBase.location
                if (location != null) {
                    val activity: Activity? = getActivity()
                    if (activity != null) try {
                        activity.runOnUiThread(object : Runnable {
                            override fun run() {
                                try {
                                    if (this@IISTrackerBase.isMotionTrail) {
                                        //motion trails on
                                        //only keep an icon on the map every 30 iterations
                                        //only keep a max of 500 icons on the map
                                        var wasOpen = false
                                        if (marker != null && marker!!.isInfoWindowShown()) {
                                            marker!!.closeInfoWindow()
                                            wasOpen = true
                                        }
                                        motionTrailCounter++
                                        if (motionTrailCounter != 30) {
                                            //at 30 we keep the trail, otherwise remove it
                                            mMapView!!.getOverlayManager().remove(marker)
                                            marker!!.onDetach(mMapView)
                                        } else {
                                            //change the icon to something that makes it obvious that it's an old location
                                            marker!!.setAlpha(0.3f)
                                            motionTrailCounter = 0
                                        }

                                        marker = Marker(mMapView)
                                        marker!!.setImage(image)
                                        marker!!.setIcon(icon)
                                        marker!!.setTitle("International Space Station")
                                        marker!!.setPosition(location)
                                        mMapView!!.controller!!.setCenter(location)
                                        marker!!.setSnippet(nf.format(location.latitude) + "," + nf.format(location.longitude))
                                        //only add it once
                                        mMapView!!.getOverlayManager().add(marker)
                                        if (wasOpen) marker!!.showInfoWindow()
                                        if (mMapView!!.getOverlayManager().size > 500) {
                                            var overlay = mMapView!!.getOverlayManager().get(1)
                                            if (overlay is Marker) {
                                                mMapView!!.getOverlayManager().remove(overlay)
                                                overlay.onDetach(mMapView)
                                                overlay = null
                                            }
                                        }
                                    } else {
                                        //motion trails are disabled
                                        //basically, we only want 1 icon on the map for the space station
                                        marker!!.setPosition(location)
                                        mMapView!!.controller!!.setCenter(location)
                                        marker!!.setSnippet(nf.format(location.latitude) + "," + nf.format(location.longitude))
                                        if (marker!!.isInfoWindowShown()) {
                                            marker!!.closeInfoWindow()
                                            marker!!.showInfoWindow()
                                        }
                                        if (!added) {
                                            //only add it once
                                            mMapView!!.getOverlayManager().add(marker)
                                            added = true
                                        }
                                    }
                                } catch (ex: Exception) {
                                    Log.e(TAG, "error updating marker", ex)
                                }
                            }
                        })
                    } catch (ex: Exception) {
                        Log.e(TAG, "error schedule task ", ex)
                    }
                }
            }
        }
        t = Timer()
        t!!.schedule(task, 1000, 1000)
    }


    private val location: GeoPoint?
        /**
         * HTTP callout to get a JSON document that represents the IIS's current location
         *
         * @return
         */
        get() {
            //sample data
            //{"timestamp": 1483742439, "iss_position": {"latitude": "-50.8416", "longitude": "-41.2701"}, "message": "success"}

            val activeNetwork = cm!!.getActiveNetworkInfo()
            val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting()

            var pt: GeoPoint? = null
            if (isConnected) {
                try {
                    val jsonObject = json.makeHttpRequest(url_select)
                    val iss_position = requireNotNull(jsonObject).get("iss_position") as JSONObject
                    val lat = iss_position.getDouble("latitude")
                    val lon = iss_position.getDouble("longitude")
                    //valid the data
                    if (lat <= 90.0 && lat >= -90.0 && lon >= -180.0 && lon <= 180.0) {
                        pt = GeoPoint(lat, lon)
                    } else Log.e(TAG, "invalid lat,lon received")
                } catch (e: Throwable) {
                    Log.e(TAG, "error fetching json", e)
                }
            }
            return pt
        }

    public override fun onPause() {
        super.onPause()
        alive = false
        if (t != null) t!!.cancel()
        t = null
    }

    public override fun onDestroyView() {
        alive = false
        if (t != null) t!!.cancel()
        t = null
        marker!!.onDetach(mMapView)
        marker = null
        super.onDestroyView()
    }


    companion object {
        const val url_select: String = "http://api.open-notify.org/iss-now.json"
    }
}

package org.osmdroid.samplefragments.data

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.GroundOverlay
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sample on how to use the ground mOverlay, which places a geospatially referenced image on the map,
 * scaling with zoom.
 * 
 * 
 * Related issues
 * [https://github.com/osmdroid/osmdroid/issues/883](https://github.com/osmdroid/osmdroid/issues/883)
 * [https://github.com/osmdroid/osmdroid/issues/684](https://github.com/osmdroid/osmdroid/issues/684)
 * created on 1/21/2018.
 * 
 * @author Alex O'Ree
 */
class WeatherGroundOverlaySample : BaseSampleFragment(), Runnable {
    //https://radar.weather.gov/ridge/standard/KDOX_loop.gif
    private val mNorthEast = GeoPoint(50.0, -127.5)
    private val mSouthWest = GeoPoint(21.0, -66.5)

    private var cm: ConnectivityManager? = null
    private var mOverlay: GroundOverlay? = null

    override val sampleTitle: String?
        get() = "Live weather for USA using Ground Overlay"

    public override fun addOverlays() {
        super.addOverlays()

        mOverlay = GroundOverlay()
        mOverlay!!.setTransparency(0.5f)
        mOverlay!!.setPosition(mNorthEast, mSouthWest)
        mMapView!!.getOverlayManager().add(mOverlay)

        cm = getActivity()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        mMapView!!.post(object : Runnable {
            override fun run() {
                val geoPoints: MutableList<GeoPoint?> = ArrayList<GeoPoint?>()
                geoPoints.add(mNorthEast)
                geoPoints.add(mSouthWest)
                mMapView!!.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), false, 50)
            }
        })

        Toast.makeText(getActivity(), "Downloading the weather image...", Toast.LENGTH_SHORT).show()
        Thread(this).start()
    }

    override fun run() {
        val activeNetwork = cm!!.getActiveNetworkInfo()
        val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting()
        if (!isConnected) {
            val act: Activity? = getActivity()
            if (act != null) {
                act.runOnUiThread(object : Runnable {
                    override fun run() {
                        Toast.makeText(getActivity(), "Cannot connect!", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            return
        }

        val con: HttpURLConnection
        var `is`: InputStream? = null
        try {
            val url = URL(URL)
            con = url.openConnection() as HttpURLConnection
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0")

            `is` = con.getInputStream()
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            val bitmap = BitmapFactory.decodeStream(`is`)
            mOverlay!!.setImage(bitmap)
            val act: Activity? = getActivity()
            if (act != null) {
                act.runOnUiThread(object : Runnable {
                    override fun run() {
                        Toast.makeText(getActivity(), "Weather image downloaded!", Toast.LENGTH_SHORT).show()
                        mMapView!!.invalidate()
                    }
                })
            }
        } catch (e: Throwable) {
            try {
                this.getActivity()!!.runOnUiThread(object : Runnable {
                    override fun run() {
                        Toast.makeText(getActivity(), "Cannot download the weather image!", Toast.LENGTH_SHORT).show()
                    }
                })
            } catch (t: Throwable) {
                Log.e(TAG, "error showing toast from failure to fetch image", t)
            }

            Log.e(TAG, "error fetching image", e)
        } finally {
            if (`is` != null) try {
                `is`.close()
            } catch (e: IOException) {
                //
            }
        }
    }

    companion object {
        const val URL: String = "https://radar.weather.gov/ridge/standard/CONUS_loop.gif"
    }
}

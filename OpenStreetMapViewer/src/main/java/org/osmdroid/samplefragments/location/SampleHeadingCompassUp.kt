package org.osmdroid.samplefragments.location

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.IOrientationConsumer
import org.osmdroid.views.overlay.compass.IOrientationProvider
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * An example on setting the device's "up" based on heading, bearing, or compass
 * Created by alex on 4/30/16.
 */
class SampleHeadingCompassUp : BaseSampleFragment(), LocationListener, IOrientationConsumer {
    var deviceOrientation: Int = 0
    var overlay: MyLocationNewOverlay? = null
    var compass: IOrientationProvider? = null
    var gpsspeed: Float = 0f
    var gpsbearing: Float = 0f
    var textViewCurrentLocation: TextView? = null
    var lat: Float = 0f
    var lon: Float = 0f
    var alt: Float = 0f
    var timeOfFix: Long = 0
    var screen_orientation: String = ""

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.map_with_locationbox, container, false)

        mMapView = root.findViewById<MapView?>(R.id.mapview)
        textViewCurrentLocation = root.findViewById<TextView?>(R.id.textViewCurrentLocation)
        return root
    }

    override val sampleTitle: String
        get() = "Heading/Compass Up"

    public override fun addOverlays() {
        overlay = MyLocationNewOverlay(mMapView)
        overlay!!.setEnableAutoStop(false)
        overlay!!.enableFollowLocation()
        overlay!!.enableMyLocation()
        this.mMapView!!.getOverlayManager().add(overlay)
    }

    public override fun onResume() {
        super.onResume()
        //hack for x86
        if (!"Android-x86".equals(Build.BRAND, ignoreCase = true)) {
            //lock the device in current screen orientation


            val orientation: Int
            val rotation = (getActivity()!!.getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager).getDefaultDisplay().getRotation()
            when (rotation) {
                Surface.ROTATION_0 -> {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    this.deviceOrientation = 0
                    screen_orientation = "ROTATION_0 SCREEN_ORIENTATION_PORTRAIT"
                }

                Surface.ROTATION_90 -> {
                    this.deviceOrientation = 90
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    screen_orientation = "ROTATION_90 SCREEN_ORIENTATION_LANDSCAPE"
                }

                Surface.ROTATION_180 -> {
                    this.deviceOrientation = 180
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    screen_orientation = "ROTATION_180 SCREEN_ORIENTATION_REVERSE_PORTRAIT"
                }

                else -> {
                    this.deviceOrientation = 270
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    screen_orientation = "ROTATION_270 SCREEN_ORIENTATION_REVERSE_LANDSCAPE"
                }
            }

            getActivity()!!.setRequestedOrientation(orientation)
        }


        val lm = getActivity()!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            //on API15 AVDs,network provider fails. no idea why
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
        } catch (ex: Exception) {
            //usually permissions or
            //java.lang.IllegalArgumentException: provider doesn't exist: network
            ex.printStackTrace()
        }
        if (compass == null) compass = InternalCompassOrientationProvider(getActivity())
        compass!!.startOrientationProvider(this)
        mMapView!!.controller!!.zoomTo(16)
    }

    public override fun onPause() {
        super.onPause()
        compass!!.stopOrientationProvider()
        val lm = getActivity()!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            lm.removeUpdates(this)
        } catch (ex: Exception) {
        }

        //unlock the orientation
        getActivity()!!.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }

    public override fun onDestroyView() {
        super.onDestroyView()
        compass!!.destroy()
        overlay!!.disableMyLocation()
        overlay!!.disableFollowLocation()
        overlay!!.onDetach(mMapView)
        if (mMapView != null) mMapView!!.onDetach()
        mMapView = null
        overlay = null
        compass = null
        textViewCurrentLocation = null
    }

    override fun onLocationChanged(location: Location) {
        if (mMapView == null) return

        gpsbearing = location.getBearing()
        gpsspeed = location.getSpeed()
        lat = location.getLatitude().toFloat()
        lon = location.getLongitude().toFloat()
        alt = location.getAltitude().toFloat() //meters
        timeOfFix = location.getTime()


        //use gps bearing instead of the compass
        var t = (360 - gpsbearing - this.deviceOrientation)
        if (t < 0) {
            t += 360f
        }
        if (t > 360) {
            t -= 360f
        }
        //help smooth everything out
        t = t.toInt().toFloat()
        t = t / 5
        t = t.toInt().toFloat()
        t = t * 5

        if (gpsspeed >= 0.01) {
            mMapView!!.setMapOrientation(t)
            //otherwise let the compass take over
        }
        updateDisplay(location.getBearing(), true)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }

    var trueNorth: Float = 0f

    override fun onOrientationChanged(orientationToMagneticNorth: Float, source: IOrientationProvider?) {
        //note, on devices without a compass this never fires...

        //only use the compass bit if we aren't moving, since gps is more accurate when we are moving

        if (gpsspeed < 0.01) {
            var gf: GeomagneticField? = GeomagneticField(lat, lon, alt, timeOfFix)
            trueNorth = orientationToMagneticNorth + gf!!.getDeclination()
            gf = null
            synchronized(trueNorth) {
                if (trueNorth > 360.0f) {
                    trueNorth = trueNorth - 360.0f
                }
                var actualHeading = 0f

                //this part adjusts the desired map rotation based on device orientation and compass heading
                var t = (360 - trueNorth - this.deviceOrientation)
                if (t < 0) {
                    t += 360f
                }
                if (t > 360) {
                    t -= 360f
                }
                actualHeading = t
                //help smooth everything out
                t = t.toInt().toFloat()
                t = t / 5
                t = t.toInt().toFloat()
                t = t * 5
                mMapView!!.setMapOrientation(t)
                updateDisplay(actualHeading, false)
            }
        }
    }

    private fun updateDisplay(bearing: Float, isGps: Boolean) {
        try {
            val act: Activity? = getActivity()
            if (act != null) act.runOnUiThread(object : Runnable {
                override fun run() {
                    if (getActivity() != null && textViewCurrentLocation != null) {
                        textViewCurrentLocation!!.setText(
                            "GPS Speed: " + gpsspeed + "m/s  GPS Bearing: " + gpsbearing +
                                    "\nDevice Orientation: " + deviceOrientation + "  Compass heading: " + bearing.toInt() + "\n" +
                                    "True north: " + trueNorth.toInt() + " Map Orientation: " + mMapView!!.getMapOrientation().toInt() + "\n" +
                                    screen_orientation
                        )
                    }
                }
            })
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        Log.i(
            TAG,
            isGps.toString() + "," + gpsspeed + "," + gpsbearing + "," + deviceOrientation + "," + bearing + "," + trueNorth.toInt() + "," + mMapView!!.getMapOrientation() + "," + screen_orientation
        )
    }
}

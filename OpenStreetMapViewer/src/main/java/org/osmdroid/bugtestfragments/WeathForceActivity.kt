package org.osmdroid.bugtestfragments

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import org.osmdroid.R
import org.osmdroid.config.Configuration.instance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.model.BaseActivity
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.IOrientationConsumer
import org.osmdroid.views.overlay.compass.IOrientationProvider
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * http://stackoverflow.com/q/40112165/1203182
 * Created by alex on 10/21/16.
 */
class WeathForceActivity : BaseActivity(), LocationListener, IOrientationConsumer, MapEventsReceiver {
    val TAG: String = "WeathForceActivity"
    private var mCompassOverlay: CompassOverlay? = null
    private var mLocationOverlay: MyLocationNewOverlay? = null
    var compass: IOrientationProvider? = null
    var deviceOrientation: Int = 0
    var mMapView: MapView? = null
    var gpsspeed: Float = 0f
    var gpsbearing: Float = 0f
    var lat: Float = 0f
    var lon: Float = 0f
    var alt: Float = 0f
    var timeOfFix: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starter_mapview)

        val intent = getIntent()
        //if (intent)
        val lat1 = 25.633
        val long1 = 71.094

        //super important. Many tile servers, including open street maps, will BAN applications by user
        //agent. Do not use the sample application's user agent for your app! Use your own setting, such
        //as the app id.
        instance!!.userAgentValue = getPackageName()

        mMapView = findViewById<MapView?>(R.id.mapview)
        mMapView!!.setTileSource(TileSourceFactory.MAPNIK)


        mCompassOverlay = CompassOverlay(
            this, InternalCompassOrientationProvider(this),
            mMapView
        )
        mCompassOverlay!!.enableCompass()
        mMapView!!.getOverlays()!!.add(this.mCompassOverlay)

        addOverlays()

        val startPoint = GeoPoint(lat1, long1)
        val mapController = mMapView!!.controller
        mapController!!.setZoom(9)
        mapController.setCenter(startPoint)
        val startMarker = Marker(mMapView!!)
        startMarker.position = startPoint
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mMapView!!.getOverlays()!!.add(startMarker)


        mMapView!!.invalidate()
    }

    fun addOverlays() {
        mLocationOverlay = MyLocationNewOverlay(mMapView!!)
        mLocationOverlay!!.enableAutoStop = false
        mLocationOverlay!!.enableFollowLocation()
        mLocationOverlay!!.enableMyLocation()
        this.mMapView!!.getOverlayManager().add(mLocationOverlay)
        mMapView!!.setMultiTouchControls(true)
        mMapView!!.setTilesScaledToDpi(true)
    }

    override fun onResume() {
        super.onResume()

        //lock the device in current screen orientation
        val orientation: Int
        val rotation = (this.getSystemService(
            WINDOW_SERVICE
        ) as WindowManager).getDefaultDisplay().getRotation()
        when (rotation) {
            Surface.ROTATION_0 -> {
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                this.deviceOrientation = 0
            }

            Surface.ROTATION_90 -> {
                this.deviceOrientation = 90
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            Surface.ROTATION_180 -> {
                this.deviceOrientation = 180
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }

            else -> {
                this.deviceOrientation = 270
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
        }

        this.setRequestedOrientation(orientation)


        val lm = this.getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
        } catch (ex: Exception) {
        }
        compass = InternalCompassOrientationProvider(this)
        compass!!.startOrientationProvider(this)
        mMapView!!.controller!!.zoomTo(14)
    }

    override fun onLocationChanged(location: Location) {
        if (mMapView == null) return
        //after the first fix, schedule the task to change the icon
        //mMapView.getController().setExpectedCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
        mMapView!!.invalidate()
        gpsbearing = location.getBearing()
        gpsspeed = location.getSpeed()
        lat = location.getLatitude().toFloat()
        lon = location.getLongitude().toFloat()
        alt = location.getAltitude().toFloat() //meters
        timeOfFix = location.getTime()
    }

    override fun onStatusChanged(s: String?, i: Int, bundle: Bundle?) {
    }

    override fun onProviderEnabled(s: String) {
    }

    override fun onProviderDisabled(s: String) {
    }

    override fun onPause() {
        super.onPause()
        compass!!.stopOrientationProvider()
        val lm = this.getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            lm.removeUpdates(this)
        } catch (ex: Exception) {
        }

        //unlock the orientation
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }


    var trueNorth: Float = 0f
    private val orientationLock = Any()

    override fun onOrientationChanged(orientationToMagneticNorth: Float, source: IOrientationProvider?) {
        var gf: GeomagneticField? = GeomagneticField(lat, lon, alt, timeOfFix)
        trueNorth = orientationToMagneticNorth + gf!!.getDeclination()
        gf = null
        synchronized(orientationLock) {
            if (trueNorth > 360.0f) {
                trueNorth = trueNorth - 360.0f
            }
            //use gps bearing instead of the compass
            if (gpsspeed > 0.01f) {
                var t = (360 - gpsbearing - this.deviceOrientation)
                if (t < 0) {
                    t += 360f
                }
                if (t > 360) {
                    t -= 360f
                }
                mMapView!!.setMapOrientation(t)
            } else {
                //this part adjusts the desired map rotation based on device orientation and compass heading

                var t = (360 - trueNorth - this.deviceOrientation)
                if (t < 0) {
                    t += 360f
                }
                if (t > 360) {
                    t -= 360f
                }
                mMapView!!.setMapOrientation(t)
            }
            this.runOnUiThread(object : Runnable {
                override fun run() {
                    if (this != null) {
                        Log.i(
                            TAG,
                            "GPS Speed: " + gpsspeed + "m/s  GPS Bearing: " + gpsbearing +
                                    "\nDevice Orientation: " + deviceOrientation + "  Compass heading: " + orientationToMagneticNorth.toInt() + "\n" +
                                    "True north: " + trueNorth.toInt() + " Map Orientation: " + mMapView!!.getMapOrientation().toInt()
                        )
                    }
                }
            })
        }
    }

    override fun singleTapConfirmedHelper(geoPoint: GeoPoint?): Boolean {
        return false
    }

    override fun longPressHelper(geoPoint: GeoPoint?): Boolean {
        return false
    }

    override val activityTitle: String
        get() = "Weather Force Test"
}

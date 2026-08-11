package org.osmdroid.samplefragments.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Based off the submission from https://github.com/2ndGAB
 *
 *
 * Source: https://github.com/2ndGAB/OSMCenterToMyPosition
 * Created by alex on 6/6/16.
 */
class SampleFollowMe : BaseSampleFragment(), LocationListener {
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var mCompassOverlay: CompassOverlay? = null
    private var mScaleBarOverlay: ScaleBarOverlay? = null
    private var mRotationGestureOverlay: RotationGestureOverlay? = null
    protected var btCenterMap: ImageButton? = null
    protected var btFollowMe: ImageButton? = null
    private var lm: LocationManager? = null
    private var currentLocation: Location? = null

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.sample_followme, null)
        mMapView = v.findViewById<MapView?>(R.id.mapview)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //super.onViewCreated(view, savedInstanceState);

        val context: Context? = this.getActivity()
        val dm = context!!.getResources().getDisplayMetrics()

        this.mCompassOverlay = CompassOverlay(
            context, InternalCompassOrientationProvider(context),
            mMapView
        )
        this.mLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(context),
            mMapView
        )

        mScaleBarOverlay = ScaleBarOverlay(mMapView)
        mScaleBarOverlay!!.setCentred(true)
        mScaleBarOverlay!!.setScaleBarOffset(dm.widthPixels / 2, 10)

        mRotationGestureOverlay = RotationGestureOverlay(mMapView)
        mRotationGestureOverlay!!.setEnabled(true)

        mMapView!!.controller!!.setZoom(15)
        mMapView!!.setTilesScaledToDpi(true)
        mMapView!!.setMultiTouchControls(true)
        mMapView!!.setFlingEnabled(true)
        mMapView!!.getOverlays()!!.add(this.mLocationOverlay)
        mMapView!!.getOverlays()!!.add(this.mCompassOverlay)
        mMapView!!.getOverlays()!!.add(this.mScaleBarOverlay)

        mLocationOverlay!!.enableMyLocation()
        mLocationOverlay!!.enableFollowLocation()
        mLocationOverlay!!.isOptionsMenuEnabled = true
        mCompassOverlay!!.enableCompass()

        btCenterMap = view.findViewById<ImageButton?>(R.id.ic_center_map)

        btCenterMap!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                Log.i(TAG, "centerMap clicked ")
                if (currentLocation != null) {
                    val myPosition = GeoPoint(currentLocation!!.getLatitude(), currentLocation!!.getLongitude())
                    mMapView!!.controller!!.animateTo(myPosition)
                }
            }
        })

        btFollowMe = view.findViewById<ImageButton?>(R.id.ic_follow_me)

        btFollowMe!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                Log.i(TAG, "btFollowMe clicked ")
                if (!mLocationOverlay!!.isFollowLocationEnabled()) {
                    mLocationOverlay!!.enableFollowLocation()
                    btFollowMe!!.setImageResource(R.drawable.ic_follow_me_on)
                } else {
                    mLocationOverlay!!.disableFollowLocation()
                    btFollowMe!!.setImageResource(R.drawable.ic_follow_me)
                }
            }
        })
    }

    public override fun onPause() {
        super.onPause()
        try {
            lm!!.removeUpdates(this)
        } catch (ex: Exception) {
        }

        mCompassOverlay!!.disableCompass()
        mLocationOverlay!!.disableFollowLocation()
        mLocationOverlay!!.disableMyLocation()
        mScaleBarOverlay!!.enableScaleBar()
    }

    public override fun onResume() {
        super.onResume()
        lm = getActivity()!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        try {
            //this fails on AVD 19s, even with the appcompat check, says no provided named gps is available
            lm!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        } catch (ex: Exception) {
        }

        try {
            lm!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, this)
        } catch (ex: Exception) {
        }

        mLocationOverlay!!.enableFollowLocation()
        mLocationOverlay!!.enableMyLocation()
        mScaleBarOverlay!!.disableScaleBar()
    }

    override val sampleTitle: String
        get() = "Follow Me"

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }

    public override fun onDestroyView() {
        super.onDestroyView()
        lm = null
        currentLocation = null

        mLocationOverlay = null
        mCompassOverlay = null
        mScaleBarOverlay = null
        mRotationGestureOverlay = null
        btCenterMap = null
        btFollowMe = null
    }
}

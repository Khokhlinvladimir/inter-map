package org.osmdroid.samplefragments.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * See https://github.com/osmdroid/osmdroid/issues/815
 * created on 12/21/2017.
 *
 * @author Alex O'Ree
 */
class SampleCustomMyLocation : BaseSampleFragment(), LocationListener {
    override val sampleTitle: String
        get() = "Custom My Location Overlay"

    var mgr: LocationManager? = null
    var myLocation: Marker? = null
    var added: Boolean = false
    var followme: Boolean = true

    public override fun addOverlays() {
        super.addOverlays()
        myLocation = Marker(mMapView!!)
        myLocation!!.icon = getResources().getDrawable(R.drawable.icon)
        myLocation!!.image = getResources().getDrawable(R.drawable.icon)
    }

    public override fun onResume() {
        super.onResume()
        mgr = getContext()!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        if (ActivityCompat.checkSelfPermission(
                getContext()!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                getContext()!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        try {
            mgr!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (mgr != null) try {
            mgr!!.removeUpdates(this)
            mgr = null
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        if (mgr != null) try {
            mgr!!.removeUpdates(this)
            mgr = null
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    override fun onLocationChanged(location: Location) {
        myLocation!!.position = GeoPoint(location.getLatitude(), location.getLongitude())
        if (!added) {
            mMapView!!.getOverlayManager().add(myLocation)
            added = true
        }
        if (followme) {
            mMapView!!.controller!!.animateTo(myLocation!!.position)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }
}

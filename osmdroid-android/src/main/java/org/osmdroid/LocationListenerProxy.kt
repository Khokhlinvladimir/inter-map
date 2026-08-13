package org.osmdroid

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class LocationListenerProxy(pLocationManager: LocationManager) : LocationListener {
    private val mLocationManager: LocationManager
    private var mListener: LocationListener? = null

    init {
        mLocationManager = pLocationManager
    }

    fun startListening(
        pListener: LocationListener?, pUpdateTime: Long,
        pUpdateDistance: Float
    ): Boolean {
        var result = false
        mListener = pListener
        for (provider in mLocationManager.getProviders(true)) {
            if (LocationManager.GPS_PROVIDER == provider
                || LocationManager.NETWORK_PROVIDER == provider
            ) {
                result = true
                mLocationManager.requestLocationUpdates(
                    provider, pUpdateTime, pUpdateDistance,
                    this
                )
            }
        }
        return result
    }

    fun stopListening() {
        mListener = null
        mLocationManager.removeUpdates(this)
    }

    override fun onLocationChanged(arg0: Location) {
        if (mListener != null) {
            mListener!!.onLocationChanged(arg0)
        }
    }

    override fun onProviderDisabled(arg0: String) {
        if (mListener != null) {
            mListener!!.onProviderDisabled(arg0)
        }
    }

    override fun onProviderEnabled(arg0: String) {
        if (mListener != null) {
            mListener!!.onProviderEnabled(arg0)
        }
    }

    override fun onStatusChanged(arg0: String?, arg1: Int, arg2: Bundle?) {
        if (mListener != null) {
            mListener!!.onStatusChanged(arg0, arg1, arg2)
        }
    }
}

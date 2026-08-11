package org.osmdroid.util

import android.location.Location
import android.location.LocationManager
import org.osmdroid.config.Configuration.instance

object LocationUtils {
    /**
     * Get the most recent location from the GPS or Network provider.
     *
     * @return return the most recent location, or null if there's no known location
     */
    fun getLastKnownLocation(pLocationManager: LocationManager?): Location? {
        if (pLocationManager == null) {
            return null
        }
        val gpsLocation = getLastKnownLocation(pLocationManager, LocationManager.GPS_PROVIDER)
        val networkLocation = getLastKnownLocation(pLocationManager, LocationManager.NETWORK_PROVIDER)
        if (gpsLocation == null) {
            return networkLocation
        } else if (networkLocation == null) {
            return gpsLocation
        } else {
            // both are non-null - use the most recent
            if (networkLocation.getTime() > gpsLocation.getTime() + instance!!.gpsWaitTime) {
                return networkLocation
            } else {
                return gpsLocation
            }
        }
    }

    private fun getLastKnownLocation(pLocationManager: LocationManager, pProvider: String): Location? {
        try {
            if (!pLocationManager.isProviderEnabled(pProvider)) {
                return null
            }
        } catch (e: IllegalArgumentException) {
            return null
        }
        return pLocationManager.getLastKnownLocation(pProvider)
    }
}

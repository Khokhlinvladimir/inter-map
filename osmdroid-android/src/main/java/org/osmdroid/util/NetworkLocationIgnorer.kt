package org.osmdroid.util

import android.location.LocationManager
import org.osmdroid.config.Configuration.instance

/**
 * A class to check whether we want to use a location. If there are multiple location providers,
 * i.e. network and GPS, then you want to ignore network locations shortly after a GPS location
 * because you will get another GPS location soon.
 */
class NetworkLocationIgnorer {
    /**
     * last time we got a location from the gps provider
     */
    private var mLastGps: Long = 0

    /**
     * Whether we should ignore this location.
     *
     * @param pProvider the provider that provided the location
     * @param pTime     the time of the location
     * @return true if we should ignore this location, false if not
     */
    fun shouldIgnore(pProvider: String?, pTime: Long): Boolean {
        if (LocationManager.GPS_PROVIDER == pProvider) {
            mLastGps = pTime
        } else {
            if (pTime < mLastGps + instance!!.gpsWaitTime) {
                return true
            }
        }

        return false
    }
}

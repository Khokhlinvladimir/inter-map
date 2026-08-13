package org.osmdroid.views.overlay.mylocation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.util.NetworkLocationIgnorer

/**
 * location provider, by default, uses [LocationManager.GPS_PROVIDER] and [LocationManager.NETWORK_PROVIDER]
 */
class GpsMyLocationProvider(context: Context) : IMyLocationProvider, LocationListener {
    private var mLocationManager: LocationManager?
    override var lastKnownLocation: Location? = null
        private set

    private var mMyLocationConsumer: IMyLocationConsumer? = null

    /**
     * Set the minimum interval for location updates. See
     * [LocationManager.requestLocationUpdates]. Note
     * that you should call this before calling [MyLocationNewOverlay.enableMyLocation].
     *
     * @param milliSeconds
     */
    var locationUpdateMinTime: Long = 0

    /**
     * Set the minimum distance for location updates. See
     * [LocationManager.requestLocationUpdates]. Note
     * that you should call this before calling [MyLocationNewOverlay.enableMyLocation].
     *
     * @param meters
     */
    var locationUpdateMinDistance: Float = 0.0f
    private var mIgnorer: NetworkLocationIgnorer? = NetworkLocationIgnorer()

    /**
     * returns the live list of GPS sources that we accept, changing this list after startLocationProvider
     * has no effect unless startLocationProvider is called again
     *
     * @return
     */
    val locationSources: MutableSet<String?> = HashSet<String?>()

    init {
        mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        locationSources.add(LocationManager.GPS_PROVIDER)
        locationSources.add(LocationManager.NETWORK_PROVIDER)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    /**
     * removes all sources, again, only useful before startLocationProvider is called
     */
    fun clearLocationSources() {
        locationSources.clear()
    }

    /**
     * adds a new source to listen for location data. Has no effect after startLocationProvider has been called
     * unless startLocationProvider is called again
     */
    fun addLocationSource(source: String?) {
        locationSources.add(source)
    }

    //
    // IMyLocationProvider
    //
    /**
     * Enable location updates and show your current location on the map. By default this will
     * request location updates as frequently as possible, but you can change the frequency and/or
     * distance by calling [.setLocationUpdateMinTime] and/or [ ][.setLocationUpdateMinDistance] before calling this method.
     */
    @SuppressLint("MissingPermission")
    override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
        mMyLocationConsumer = myLocationConsumer
        var result = false
        for (provider in mLocationManager!!.getProviders(true)) {
            if (locationSources.contains(provider)) {
                try {
                    mLocationManager!!.requestLocationUpdates(
                        provider, this.locationUpdateMinTime,
                        this.locationUpdateMinDistance, this
                    )
                    result = true
                } catch (ex: Throwable) {
                    Log.e(IMapView.LOGTAG, "Unable to attach listener for location provider " + provider + " check permissions?", ex)
                }
            }
        }
        return result
    }

    @SuppressLint("MissingPermission")
    override fun stopLocationProvider() {
        mMyLocationConsumer = null
        if (mLocationManager != null) {
            try {
                mLocationManager!!.removeUpdates(this)
            } catch (ex: Throwable) {
                Log.w(IMapView.LOGTAG, "Unable to deattach location listener", ex)
            }
        }
    }

    override fun destroy() {
        stopLocationProvider()
        this.lastKnownLocation = null
        mLocationManager = null
        mMyLocationConsumer = null
        mIgnorer = null
    }

    //
    // LocationListener
    //
    override fun onLocationChanged(location: Location) {
        if (mIgnorer == null) {
            Log.w(IMapView.LOGTAG, "GpsMyLocation provider, mIgnore is null, unexpected. Location update will be ignored")
            return
        }
        if (location.getProvider() == null) return
        // ignore temporary non-gps fix
        if (mIgnorer!!.shouldIgnore(location.getProvider(), System.currentTimeMillis())) return

        this.lastKnownLocation = location
        if (mMyLocationConsumer != null && this.lastKnownLocation != null) mMyLocationConsumer!!.onLocationChanged(this.lastKnownLocation, this)
    }

    override fun onProviderDisabled(provider: String) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }
}

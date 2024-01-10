package org.osmdroid.views.overlay.mylocation

import android.location.Location

interface IMyLocationConsumer {
    /**
     * Call when a provider has a new location to consume. This can be called on any thread.
     */
    fun onLocationChanged(location: Location?, source: IMyLocationProvider?)
}
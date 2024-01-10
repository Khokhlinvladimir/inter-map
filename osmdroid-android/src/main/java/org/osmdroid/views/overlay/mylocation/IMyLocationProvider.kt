package org.osmdroid.views.overlay.mylocation

import android.location.Location

interface IMyLocationProvider {
    fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean
    fun stopLocationProvider()
    val lastKnownLocation: Location?
    fun destroy()
}
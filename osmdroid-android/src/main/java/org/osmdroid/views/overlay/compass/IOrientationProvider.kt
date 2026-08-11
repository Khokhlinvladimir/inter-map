package org.osmdroid.views.overlay.compass


interface IOrientationProvider {
    fun startOrientationProvider(orientationConsumer: IOrientationConsumer?): Boolean

    fun stopOrientationProvider()

    val lastKnownOrientation: Float

    fun destroy()
}

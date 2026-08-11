package org.osmdroid.views.overlay.compass


interface IOrientationConsumer {
    /**
     * @param orientation this is magnetic north, not true north
     * @param source
     */
    fun onOrientationChanged(orientation: Float, source: IOrientationProvider?)
}

package org.osmdroid.tileprovider.modules

interface INetworkAvailablityCheck {
    val networkAvailable: Boolean
    val wiFiNetworkAvailable: Boolean
    val cellularDataNetworkAvailable: Boolean

    /**
     * this method calls a method that was removed API26
     * and this method will be removed from osmdroid sometime after
     * v6.0.0.
     *
     * @param hostAddress
     * @return
     */
    @Deprecated("")
    fun getRouteToPathExists(hostAddress: Int): Boolean
}
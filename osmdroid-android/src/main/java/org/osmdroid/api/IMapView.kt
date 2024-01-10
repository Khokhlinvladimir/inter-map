package org.osmdroid.api

/**
 * An interface that resembles the Google Maps API MapView class
 * and is implemented by the osmdroid [org.osmdroid.views.MapView] class.
 *
 * @author Neil Boyd
 */
interface IMapView {
    val controller: IMapController?
    val projection: IProjection?

    @get:Deprecated("")
    val zoomLevel: Int

    /**
     * @since 6.0
     */
    val zoomLevelDouble: Double
    val maxZoomLevel: Double
    val latitudeSpanDouble: Double
    val longitudeSpanDouble: Double
    val mapCenter: IGeoPoint?

    // some methods from View
    // (well, just one for now)
    fun setBackgroundColor(color: Int)

    companion object {
        const val LOGTAG = "OsmDroid"
    }
}
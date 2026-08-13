package org.osmdroid.samplefragments.data

import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.cluster.GridMarkerClusterer
import java.util.Random

/** Demonstrates the core grid-clustering overlay with a few hundred interactive markers. */
class SampleMarkerClustering : BaseSampleFragment() {
    private var clusterer: GridMarkerClusterer? = null

    override val sampleTitle: String
        get() = "Marker clustering"

    override fun addOverlays() {
        super.addOverlays()
        val mapView = mMapView ?: return
        val random = Random(RANDOM_SEED)
        val markerIcon = resources.getDrawable(R.drawable.marker_default)
        clusterer = GridMarkerClusterer(mapView).apply {
            maximumClusterZoom = 17.0
            addAll(
                List(MARKER_COUNT) { index ->
                    Marker(mapView).apply {
                        position = GeoPoint(
                            MOSCOW.latitude + (random.nextDouble() - 0.5) * LATITUDE_SPAN,
                            MOSCOW.longitude + (random.nextDouble() - 0.5) * LONGITUDE_SPAN
                        )
                        icon = markerIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setTitle("Place ${index + 1}")
                        setSnippet("Zoom in to split this cluster into individual markers.")
                    }
                }
            )
        }
        mapView.getOverlays()?.add(clusterer)
        mapView.controller?.setZoom(10.0)
        mapView.controller?.setCenter(MOSCOW)
    }

    override fun runTestProcedures() {
        check(clusterer?.size() == MARKER_COUNT) { "Not all clustering markers were added" }
    }

    companion object {
        private const val MARKER_COUNT = 1200
        private const val RANDOM_SEED = 20260811L
        private const val LATITUDE_SPAN = 0.8
        private const val LONGITUDE_SPAN = 1.2
        private val MOSCOW = GeoPoint(55.751244, 37.618423)
    }
}

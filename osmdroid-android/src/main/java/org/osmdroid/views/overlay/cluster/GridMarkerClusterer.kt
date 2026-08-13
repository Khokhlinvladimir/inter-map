package org.osmdroid.views.overlay.cluster

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

fun interface ClusterIconFactory {
    fun createIcon(clusterSize: Int, mapView: MapView): Drawable
}

fun interface OnClusterClickListener {
    fun onClusterClick(cluster: MapCluster<Marker>, mapView: MapView): Boolean
}

/**
 * An osmdroid overlay that displays nearby [Marker] instances as a single cluster marker.
 *
 * Clusters are rebuilt only when their inputs or integer zoom level change. Source markers keep
 * their own click, info-window, and drag behavior when displayed individually. Cluster clicks zoom
 * to the contained markers by default.
 */
class GridMarkerClusterer @JvmOverloads constructor(
    mapView: MapView,
    cellSizeDp: Float = DEFAULT_CELL_SIZE_DP
) : Overlay() {
    private val mapViewReference = WeakReference(mapView)
    private val sourceMarkers = CopyOnWriteArrayList<Marker>()
    private val generatedMarkers = ArrayList<Marker>()
    private val algorithm = GridClusterAlgorithm(
        cellSizePixels = max(1, (cellSizeDp * mapView.resources.displayMetrics.density).toInt())
    )

    @Volatile
    private var displayedMarkers: List<Marker> = emptyList()
    private var revision = 0L
    private var lastStateHash = Long.MIN_VALUE
    private var lastZoom = Double.NaN
    private var lastReferenceLongitude = Double.NaN

    var cellSizePixels: Int
        get() = algorithm.cellSizePixels
        set(value) {
            require(value > 0) { "cellSizePixels must be greater than zero" }
            algorithm.cellSizePixels = value
            invalidateClusters()
        }

    var minimumClusterSize: Int
        get() = algorithm.minimumClusterSize
        set(value) {
            require(value > 1) { "minimumClusterSize must be greater than one" }
            algorithm.minimumClusterSize = value
            invalidateClusters()
        }

    var maximumClusterZoom: Double
        get() = algorithm.maximumClusterZoom
        set(value) {
            algorithm.maximumClusterZoom = value
            invalidateClusters()
        }

    var iconFactory: ClusterIconFactory = DefaultClusterIconFactory()
        set(value) {
            field = value
            invalidateClusters()
        }

    var onClusterClickListener: OnClusterClickListener? = null

    fun add(marker: Marker): Boolean {
        val added = sourceMarkers.addIfAbsent(marker)
        if (added) invalidateClusters()
        return added
    }

    fun addAll(markers: Collection<Marker>): Boolean {
        var changed = false
        markers.forEach { changed = sourceMarkers.addIfAbsent(it) || changed }
        if (changed) invalidateClusters()
        return changed
    }

    fun remove(marker: Marker): Boolean {
        val removed = sourceMarkers.remove(marker)
        if (removed) invalidateClusters()
        return removed
    }

    fun clear() {
        if (sourceMarkers.isNotEmpty()) {
            sourceMarkers.clear()
            invalidateClusters()
        }
    }

    fun size(): Int = sourceMarkers.size

    fun markers(): List<Marker> = sourceMarkers.toList()

    /** Call after changing marker positions in bulk to force an immediate cluster rebuild. */
    fun invalidateClusters() {
        revision++
        lastStateHash = Long.MIN_VALUE
        mapViewReference.get()?.invalidate()
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        rebuildIfNeeded(mapView)
        displayedMarkers.forEach { it.draw(canvas, mapView, false) }
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        displayedMarkers.forEach { it.draw(canvas, projection) }
    }

    override fun onSingleTapConfirmed(event: MotionEvent, mapView: MapView?): Boolean =
        dispatchInReverse { it.onSingleTapConfirmed(event, mapView) }

    override fun onLongPress(event: MotionEvent, mapView: MapView?): Boolean =
        dispatchInReverse { it.onLongPress(event, mapView) }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView?): Boolean =
        dispatchInReverse { it.onTouchEvent(event, mapView) }

    override fun getBounds(): BoundingBox {
        if (sourceMarkers.isEmpty()) return super.getBounds()
        return boundingBox(sourceMarkers.map { it.position })
    }

    override fun onDetach(mapView: MapView?) {
        generatedMarkers.forEach { it.onDetach(mapView) }
        sourceMarkers.forEach { it.onDetach(mapView) }
        generatedMarkers.clear()
        displayedMarkers = emptyList()
        mapViewReference.clear()
    }

    private fun rebuildIfNeeded(mapView: MapView) {
        val zoom = mapView.zoomLevelDouble
        val referenceLongitude = mapView.mapCenter?.longitude ?: 0.0
        val stateHash = markerStateHash()
        val referenceMoved = lastReferenceLongitude.isNaN() ||
            longitudeDistance(lastReferenceLongitude, referenceLongitude) >= REFERENCE_REBUILD_DEGREES
        val zoomMoved = lastZoom.isNaN() || abs(zoom - lastZoom) >= ZOOM_REBUILD_DELTA
        if (!zoomMoved && stateHash == lastStateHash && !referenceMoved) return

        generatedMarkers.forEach { it.onDetach(mapView) }
        generatedMarkers.clear()
        val clusters = algorithm.cluster(
            sourceMarkers.map { ClusterPoint(it, it.position) },
            zoom,
            referenceLongitude
        )
        displayedMarkers = clusters.map { cluster ->
            if (cluster.items.size == 1) cluster.items.first() else createClusterMarker(cluster, mapView)
        }
        lastZoom = zoom
        lastStateHash = stateHash
        lastReferenceLongitude = referenceLongitude
    }

    private fun createClusterMarker(cluster: MapCluster<Marker>, mapView: MapView): Marker {
        return Marker(mapView).apply {
            position = cluster.position
            icon = iconFactory.createIcon(cluster.items.size, mapView)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setTitle("${cluster.items.size} markers")
            setRelatedObject(cluster)
            setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                override fun onMarkerClick(marker: Marker?, clickedMapView: MapView?): Boolean {
                    val target = clickedMapView ?: return false
                    val listener = onClusterClickListener
                    return listener?.onClusterClick(cluster, target) ?: zoomToCluster(cluster, target)
                }
            })
            generatedMarkers.add(this)
        }
    }

    private fun zoomToCluster(cluster: MapCluster<Marker>, mapView: MapView): Boolean {
        val positions = cluster.items.map { it.position }
        val bounds = boundingBox(positions)
        val samePoint = bounds.latitudeSpan == 0.0 && bounds.longitudeSpanWithDateLine == 0.0
        if (samePoint) {
            mapView.controller?.animateTo(cluster.position)
            mapView.controller?.setZoom(min(mapView.maxZoomLevel, mapView.zoomLevelDouble + 2.0))
        } else {
            mapView.zoomToBoundingBox(bounds, true, (DEFAULT_CLUSTER_PADDING_DP * mapView.resources.displayMetrics.density).toInt())
        }
        return true
    }

    private fun boundingBox(points: List<GeoPoint>): BoundingBox {
        val centerLongitude = circularLongitude(points)
        val unwrapped = points.map { unwrapLongitude(it.longitude, centerLongitude) }
        val west = unwrapped.minOrNull() ?: centerLongitude
        val east = unwrapped.maxOrNull() ?: centerLongitude
        return BoundingBox(
            points.maxOf { it.latitude },
            normalizeLongitude(east),
            points.minOf { it.latitude },
            normalizeLongitude(west)
        )
    }

    private fun circularLongitude(points: List<GeoPoint>): Double {
        val x = points.sumOf { kotlin.math.cos(Math.toRadians(it.longitude)) }
        val y = points.sumOf { kotlin.math.sin(Math.toRadians(it.longitude)) }
        return Math.toDegrees(kotlin.math.atan2(y, x))
    }

    private fun markerStateHash(): Long {
        var hash = revision
        sourceMarkers.forEach { marker ->
            hash = 31 * hash + System.identityHashCode(marker)
            hash = 31 * hash + marker.position.latitude.toBits()
            hash = 31 * hash + marker.position.longitude.toBits()
        }
        hash = 31 * hash + algorithm.cellSizePixels
        hash = 31 * hash + algorithm.minimumClusterSize
        hash = 31 * hash + algorithm.maximumClusterZoom.toBits()
        return hash
    }

    private fun dispatchInReverse(block: (Marker) -> Boolean): Boolean {
        for (index in displayedMarkers.indices.reversed()) {
            if (block(displayedMarkers[index])) return true
        }
        return false
    }

    private fun longitudeDistance(first: Double, second: Double): Double =
        abs(normalizeLongitude(first - second))

    private fun unwrapLongitude(longitude: Double, referenceLongitude: Double): Double {
        var result = longitude
        while (result - referenceLongitude > 180.0) result -= 360.0
        while (result - referenceLongitude < -180.0) result += 360.0
        return result
    }

    private fun normalizeLongitude(longitude: Double): Double {
        var result = longitude
        while (result > 180.0) result -= 360.0
        while (result < -180.0) result += 360.0
        return result
    }

    companion object {
        const val DEFAULT_CELL_SIZE_DP = 72f
        const val DEFAULT_CLUSTER_PADDING_DP = 48f
        private const val REFERENCE_REBUILD_DEGREES = 90.0
        private const val ZOOM_REBUILD_DELTA = 0.25
    }
}

class DefaultClusterIconFactory @JvmOverloads constructor(
    private val fillColor: Int = Color.rgb(49, 101, 190),
    private val strokeColor: Int = Color.WHITE,
    private val textColor: Int = Color.WHITE
) : ClusterIconFactory {
    private val bitmaps = ConcurrentHashMap<String, Bitmap>()

    override fun createIcon(clusterSize: Int, mapView: MapView): Drawable {
        val label = if (clusterSize > 999) "999+" else clusterSize.toString()
        val density = mapView.resources.displayMetrics.density
        val diameter = (48f * density).toInt().coerceAtLeast(1)
        val key = "$label@$diameter:$fillColor:$strokeColor:$textColor"
        val bitmap = bitmaps.getOrPut(key) {
            Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888).also { target ->
                val canvas = Canvas(target)
                val center = diameter / 2f
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = strokeColor
                    style = Paint.Style.STROKE
                    strokeWidth = max(2f, 2f * density)
                }
                val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textAlign = Paint.Align.CENTER
                    textSize = 16f * density
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawCircle(center, center, center - stroke.strokeWidth, fill)
                canvas.drawCircle(center, center, center - stroke.strokeWidth, stroke)
                canvas.drawText(label, center, center - (text.ascent() + text.descent()) / 2f, text)
            }
        }
        return BitmapDrawable(mapView.resources, bitmap)
    }
}

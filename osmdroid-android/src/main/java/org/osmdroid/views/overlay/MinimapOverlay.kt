package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.view.MotionEvent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.util.constants.OverlayConstants

/**
 * Draws a mini-map as an overlay layer. It currently uses its own MapTileProviderBasic or a tile
 * provider supplied to it. Do NOT share a tile provider amongst multiple tile drawing overlays - it
 * will create an under-sized cache.
 *
 *
 * Notice, this class has some problems when the parent map view is rotation enabled.
 * See https://github.com/osmdroid/osmdroid/issues/98 for a work around
 *
 * @author Marc Kurtz
 */
class MinimapOverlay @JvmOverloads constructor(
    pContext: Context,
    pTileRequestCompleteHandler: Handler?,
    pTileProvider: MapTileProviderBase? = MapTileProviderBasic(pContext),
    pZoomDifference: Int = OverlayConstants.Companion.DEFAULT_ZOOMLEVEL_MINIMAP_DIFFERENCE
) : TilesOverlay(requireNotNull(pTileProvider), pContext) {
    /**
     * Gets the width of the mini-map in pixels
     *
     * @return the width in pixels
     */
    /**
     * Sets the width of the mini-map in pixels
     *
     * @param width the width to set in pixels
     */
    var width: Int = 100
    /**
     * Gets the height of the mini-map in pixels
     *
     * @return the height in pixels
     */
    /**
     * Sets the height of the mini-map in pixels
     *
     * @param height the height to set in pixels
     */
    var height: Int = 100
    /**
     * Gets the number of pixels from the lower-right corner to offset the mini-map
     *
     * @return the padding in pixels
     */
    /**
     * Sets the number of pixels from the lower-right corner to offset the mini-map
     *
     * @param padding the padding to set in pixels
     */
    var padding: Int = 10
    var zoomDifference: Int = 0
    private val mPaint: Paint

    /**
     * Creates a [MinimapOverlay] with the supplied tile provider. The [Handler] passed
     * in is typically the same handler being used by the main map. The [MapTileProviderBase]
     * passed in cannot be the same tile provider used in the [TilesOverlay], it must be a new
     * instance.
     *
     * @param pContext                    a context
     * @param pTileRequestCompleteHandler a handler for the tile request complete notifications
     * @param pTileProvider               a tile provider
     */
    /**
     * Creates a [MinimapOverlay] with the supplied tile provider. The [Handler] passed
     * in is typically the same handler being used by the main map. The [MapTileProviderBase]
     * passed in cannot be the same tile provider used in the [TilesOverlay], it must be a new
     * instance.
     *
     * @param pContext                    a context
     * @param pTileRequestCompleteHandler a handler for the tile request complete notifications
     * @param pTileProvider               a tile provider
     */
    /**
     * Creates a [MinimapOverlay] that uses its own [MapTileProviderBasic]. The
     * [Handler] passed in is typically the same handler being used by the main map.
     *
     * @param pContext                    a context
     * @param pTileRequestCompleteHandler a handler for tile request complete notifications
     */
    init {
        this.zoomDifference = pZoomDifference

        mTileProvider.tileRequestCompleteHandlers.add(pTileRequestCompleteHandler)

        // Don't draw loading lines in the minimap
        loadingLineColor = loadingBackgroundColor

        // Scale the default size
        val density = pContext.getResources().getDisplayMetrics().density
        this.width = (this.width * density).toInt()
        this.height = (this.height * density).toInt()

        mPaint = Paint()
        mPaint.setColor(Color.GRAY)
        mPaint.setStyle(Paint.Style.FILL)
        mPaint.setStrokeWidth(2f)
    }

    fun setTileSource(pTileSource: ITileSource?) {
        mTileProvider.setTileSource(pTileSource)
    }

    override fun draw(c: Canvas, pProjection: Projection) {
        if (!setViewPort(c, pProjection)) {
            return
        }

        // Draw a solid background where the minimap will be drawn with a 2 pixel inset
        pProjection.save(c, false, true)
        c.drawRect(
            (canvasRect!!.left - 2).toFloat(), (canvasRect!!.top - 2).toFloat(),
            (canvasRect!!.right + 2).toFloat(), (canvasRect!!.bottom + 2).toFloat(), mPaint
        )

        super.drawTiles(c, projection, projection.zoomLevel, mViewPort)
        pProjection.restore(c, true)
    }

    override fun onSingleTapUp(pEvent: MotionEvent, pMapView: MapView?): Boolean {
        // Consume event so layers underneath don't receive
        return contains(pEvent)
    }

    override fun onDoubleTap(pEvent: MotionEvent, pMapView: MapView?): Boolean {
        // Consume event so layers underneath don't receive
        return contains(pEvent)
    }

    override fun onLongPress(pEvent: MotionEvent, pMapView: MapView?): Boolean {
        // Consume event so layers underneath don't receive
        return contains(pEvent)
    }

    override var isOptionsMenuEnabled: Boolean
        get() =// Don't provide menu items from TilesOverlay.
            false
        set(value) = Unit

    private fun contains(pEvent: MotionEvent): Boolean {
        val canvasRect = this.canvasRect
        return canvasRect != null && canvasRect.contains(pEvent.getX().toInt(), pEvent.getY().toInt())
    }

    override fun setViewPort(pCanvas: Canvas, pProjection: Projection): Boolean {
        val zoomLevel = pProjection.zoomLevel - this.zoomDifference
        if (zoomLevel < mTileProvider.getMinimumZoomLevel()) {
            return false
        }

        val left = pCanvas.getWidth() - this.padding - this.width
        val top = pCanvas.getHeight() - this.padding - this.height
        canvasRect = Rect(left, top, left + this.width, top + this.height)
        projection = pProjection.getOffspring(zoomLevel, canvasRect!!)
        projection.getMercatorViewPort(mViewPort)
        return true
    }
}

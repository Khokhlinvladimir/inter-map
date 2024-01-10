package org.osmdroid.api

/**
 * An interface that resembles the Google Maps API MapController class and is implemented by the
 * osmdroid [org.osmdroid.views.MapController] class.
 *
 * @author Neil Boyd
 */
interface IMapController {
    fun animateTo(geoPoint: IGeoPoint?)
    fun animateTo(x: Int, y: Int)
    fun scrollBy(x: Int, y: Int)
    fun setCenter(point: IGeoPoint?)

    @Deprecated("")
    fun setZoom(zoomLevel: Int): Int

    /**
     * @since 6.0
     */
    fun setZoom(pZoomLevel: Double): Double
    fun stopAnimation(jumpToFinish: Boolean)
    fun stopPanning()

    /**
     * zooms in 1 whole map zoom level
     *
     * @return
     */
    fun zoomIn(): Boolean

    /**
     * zooms in 1 whole map zoom level with an adjustable zoom in animation speed
     *
     * @param animationSpeed in ms, if null the default is used
     * @return
     * @since 6.0
     */
    fun zoomIn(animationSpeed: Long?): Boolean
    fun zoomInFixing(xPixel: Int, yPixel: Int, zoomAnimation: Long?): Boolean

    /**
     * zooms in and centers the map to the given canvas coordinates
     *
     * @param xPixel
     * @param yPixel
     * @return
     */
    fun zoomInFixing(xPixel: Int, yPixel: Int): Boolean

    /**
     * zooms out 1 whole  map zoom level with adjustable zoom speed
     *
     * @param animationSpeed in ms, if null the default is used
     * @return
     * @since 6.0
     */
    fun zoomOut(animationSpeed: Long?): Boolean

    /**
     * zooms out 1 whole map zoom level
     *
     * @return
     */
    fun zoomOut(): Boolean

    /**
     * zooms out while centering the map canvas coordinates
     *
     * @param xPixel
     * @param yPixel
     * @return
     */
    fun zoomOutFixing(xPixel: Int, yPixel: Int): Boolean

    /**
     * zooms to the given zoom level (whole number) and animates the zoom motion
     *
     * @param zoomLevel 0-Max zoom of the current map tile source, typically 22 or less
     * @return
     */
    @Deprecated("")
    fun zoomTo(zoomLevel: Int): Boolean

    /**
     * zooms to the given zoom level (whole number) and animates the zoom motion with adjustable zoom speed
     *
     * @param zoomLevel      0-Max zoom of the current map tile source, typically 22 or less
     * @param animationSpeed if null, the default is used
     * @return
     * @since 6.0
     */
    fun zoomTo(zoomLevel: Int, animationSpeed: Long?): Boolean
    fun zoomToFixing(zoomLevel: Int, xPixel: Int, yPixel: Int, zoomAnimationSpeed: Long?): Boolean
    fun zoomTo(pZoomLevel: Double, animationSpeed: Long?): Boolean

    /**
     * zooms to the given zoom level
     *
     * @param pZoomLevel any real number between 0 and max zoom of the current tile source, typically 22 or less
     * @return
     */
    fun zoomTo(pZoomLevel: Double): Boolean

    @Deprecated("")
    fun zoomToFixing(zoomLevel: Int, xPixel: Int, yPixel: Int): Boolean
    fun zoomToFixing(zoomLevel: Double, xPixel: Int, yPixel: Int, zoomAnimationSpeed: Long?): Boolean

    /**
     * @since 6.0
     */
    fun zoomToFixing(pZoomLevel: Double, pXPixel: Int, pYPixel: Int): Boolean

    @Deprecated("")
    fun zoomToSpan(latSpanE6: Int, lonSpanE6: Int)
    fun zoomToSpan(latSpan: Double, lonSpan: Double)

    /**
     * @param point
     * @param pZoom
     * @param pSpeed
     * @since 6.0.2
     */
    fun animateTo(point: IGeoPoint?, pZoom: Double?, pSpeed: Long?)

    /**
     * @since 6.0.3
     */
    fun animateTo(point: IGeoPoint?, pZoom: Double?, pSpeed: Long?, pOrientation: Float?)

    /**
     * @since 6.1.0
     */
    fun animateTo(point: IGeoPoint?, pZoom: Double?, pSpeed: Long?, pOrientation: Float?, pClockwise: Boolean?)
}
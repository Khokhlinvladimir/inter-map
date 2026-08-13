package org.osmdroid.views.overlay

import android.graphics.Color
import android.graphics.Paint
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow


/**
 * A polygon on the earth's surface that can have a
 * popup-[InfoWindow] (a bubble).
 *
 *
 * Mimics the Polygon class from Google Maps Android API v2 as much as possible. Main differences:<br></br>
 * - Doesn't support: Z-Index, Geodesic mode<br></br>
 * - Supports InfoWindow.
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='src=' .></img>/doc-files/marker-infowindow-classes.png' />
 *
 * @author Viesturs Zarins, Martin Pearman for efficient PathOverlay.draw method
 * @author M.Kergall: transformation from PathOverlay to Polygon
 * @see [Google Maps Polygon](http://developer.android.com/reference/com/google/android/gms/maps/model/Polygon.html)
 */
class Polygon @JvmOverloads constructor(mapView: MapView? = null) : PolyOverlayWithIW(mapView, true, true) {
    protected var mOnClickListener: OnClickListener? = null


    // ===========================================================
    // Constructors
    // ===========================================================
    init {
        mFillPaint = Paint()
        mFillPaint!!.setColor(Color.TRANSPARENT)
        mFillPaint!!.setStyle(Paint.Style.FILL)
        mOutlinePaint.setColor(Color.BLACK)
        mOutlinePaint.setStrokeWidth(10.0f)
        mOutlinePaint.setStyle(Paint.Style.STROKE)
        mOutlinePaint.setAntiAlias(true)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    @get:Deprecated("Use {@link #getFillPaint()} instead")
    @set:Deprecated("Use {@link #getFillPaint()} instead")
    var fillColor: Int
        get() = mFillPaint!!.getColor()
        set(fillColor) {
            mFillPaint!!.setColor(fillColor)
        }

    @get:Deprecated("Use {@link #getOutlinePaint()} instead")
    @set:Deprecated("Use {@link #getOutlinePaint()} instead")
    var strokeColor: Int
        get() = mOutlinePaint.getColor()
        set(color) {
            mOutlinePaint.setColor(color)
        }

    @get:Deprecated("Use {@link #getOutlinePaint()} instead")
    @set:Deprecated("Use {@link #getOutlinePaint()} instead")
    var strokeWidth: Float
        get() = mOutlinePaint.getStrokeWidth()
        set(width) {
            mOutlinePaint.setStrokeWidth(width)
        }

    /**
     * @return the Paint used for the filling. This allows to set advanced Paint settings.
     * @since 6.0.2
     */
    public override fun getFillPaint(): Paint? {
        return super.getFillPaint() // public instead of protected
    }

    @get:Deprecated("Use {@link PolyOverlayWithIW#getActualPoints()} instead")
    val points: MutableList<GeoPoint>
        /**
         * @return the list of polygon's vertices.
         * Warning: changes on this list may cause strange results on the polygon display.
         */
        get() = getActualPoints()

    fun setHoles(holes: List<List<GeoPoint>>) {
        mHoles = ArrayList<LinearRing>(holes.size)
        for (sourceHole in holes) {
            val newHole = LinearRing(mPath!!)
            newHole.setGeodesic(mOutline!!.isGeodesic())
            newHole.setPoints(sourceHole)
            mHoles!!.add(newHole)
        }
    }

    val holes: MutableList<MutableList<GeoPoint>>
        /**
         * returns a copy of the holes this polygon contains
         *
         * @return never null
         */
        get() {
            val result: MutableList<MutableList<GeoPoint>> =
                ArrayList(mHoles!!.size)
            for (hole in mHoles!!) {
                result.add(hole.getPoints())
                //TODO: completely wrong:
                // hole.getPoints() doesn't return a copy but a direct handler to the internal list.
                // - if geodesic, this is not the same points as the original list.
            }
            return result
        }

    override fun onDetach(mapView: MapView?) {
        super.onDetach(mapView)
        mOnClickListener = null
    }


    //-- Polygon events listener interfaces ------------------------------------
    interface OnClickListener {
        fun onClick(polygon: Polygon?, mapView: MapView?, eventPos: GeoPoint?): Boolean
    }

    /**
     * default behaviour when no click listener is set
     */
    fun onClickDefault(polygon: Polygon, mapView: MapView?, eventPos: GeoPoint?): Boolean {
        polygon.setInfoWindowLocation(eventPos)
        polygon.showInfoWindow()
        return true
    }

    /**
     * @param listener
     * @since 6.0.2
     */
    fun setOnClickListener(listener: OnClickListener?) {
        mOnClickListener = listener
    }

    /**
     * @since 6.2.0
     */
    override fun click(pMapView: MapView?, pEventPos: GeoPoint?): Boolean {
        if (mOnClickListener == null) {
            return onClickDefault(this, pMapView, pEventPos)
        } else {
            return mOnClickListener!!.onClick(this, pMapView, pEventPos)
        }
    }

    companion object {
        /**
         * Build a list of GeoPoint as a circle.
         *
         * @param center         center of the circle
         * @param radiusInMeters
         * @return the list of GeoPoint
         */
        fun pointsAsCircle(center: GeoPoint, radiusInMeters: Double): ArrayList<GeoPoint?> {
            val circlePoints = ArrayList<GeoPoint?>(360 / 6)
            var f = 0
            while (f < 360) {
                val onCircle = center.destinationPoint(radiusInMeters, f.toDouble())
                circlePoints.add(onCircle)
                f += 6
            }
            return circlePoints
        }

        /**
         * Build a list of GeoPoint as a rectangle.
         *
         * @param rectangle defined as a BoundingBox
         * @return the list of 4 GeoPoint
         */
        fun pointsAsRect(rectangle: BoundingBox): ArrayList<IGeoPoint?> {
            val points = ArrayList<IGeoPoint?>(4)
            points.add(GeoPoint(rectangle.latNorth, rectangle.lonWest))
            points.add(GeoPoint(rectangle.latNorth, rectangle.lonEast))
            points.add(GeoPoint(rectangle.latSouth, rectangle.lonEast))
            points.add(GeoPoint(rectangle.latSouth, rectangle.lonWest))
            return points
        }

        /**
         * Build a list of GeoPoint as a rectangle.
         *
         * @param center         of the rectangle
         * @param lengthInMeters on longitude
         * @param widthInMeters  on latitude
         * @return the list of 4 GeoPoint
         */
        fun pointsAsRect(center: GeoPoint, lengthInMeters: Double, widthInMeters: Double): ArrayList<IGeoPoint?> {
            val points = ArrayList<IGeoPoint?>(4)
            val east = center.destinationPoint(lengthInMeters * 0.5, 90.0)
            val south = center.destinationPoint(widthInMeters * 0.5, 180.0)
            val westLon = center.longitude * 2 - east.longitude
            val northLat = center.latitude * 2 - south.latitude
            points.add(GeoPoint(south.latitude, east.longitude))
            points.add(GeoPoint(south.latitude, westLon))
            points.add(GeoPoint(northLat, westLon))
            points.add(GeoPoint(northLat, east.longitude))
            return points
        }
    }
}

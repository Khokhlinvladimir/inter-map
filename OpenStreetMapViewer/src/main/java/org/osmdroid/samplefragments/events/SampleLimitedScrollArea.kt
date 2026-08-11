package org.osmdroid.samplefragments.events

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

/**
 * @author Marc Kurtz
 */
class SampleLimitedScrollArea : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    private val MENU_LIMIT_SCROLLING_LAT_ID = Menu.FIRST
    private val MENU_LIMIT_SCROLLING_LNG_ID = Menu.FIRST + 1

    private val sCentralParkBoundingBox: BoundingBox

    // ===========================================================
    // Constructors
    // ===========================================================
    //note that since we are not providing the mapview as a constructor parameter,
    //the infowindow bubble will not be available
    private val mNorthPolyline = Polyline()
    private val mSouthPolyline = Polyline()
    private val mWestPolyline = Polyline()
    private val mEastPolyline = Polyline()


    init {
        sCentralParkBoundingBox = BoundingBox(
            40.796788,
            -73.949232, 40.768094, -73.981762
        )
    }

    override fun addOverlays() {
        super.addOverlays()

        val list = ArrayList<GeoPoint>()

        list.clear()
        list.add(GeoPoint(sCentralParkBoundingBox.actualNorth, -85.0))
        list.add(GeoPoint(sCentralParkBoundingBox.actualNorth, -65.0))
        mNorthPolyline.setPoints(list)
        mMapView!!.getOverlays()!!.add(mNorthPolyline)

        list.clear()
        list.add(GeoPoint(sCentralParkBoundingBox.actualSouth, -85.0))
        list.add(GeoPoint(sCentralParkBoundingBox.actualSouth, -65.0))
        mSouthPolyline.setPoints(list)
        mMapView!!.getOverlays()!!.add(mSouthPolyline)

        list.clear()
        list.add(GeoPoint(45.0, sCentralParkBoundingBox.lonWest))
        list.add(GeoPoint(35.0, sCentralParkBoundingBox.lonWest))
        mWestPolyline.setPoints(list)
        mMapView!!.getOverlays()!!.add(mWestPolyline)

        list.clear()
        list.add(GeoPoint(45.0, sCentralParkBoundingBox.lonEast))
        list.add(GeoPoint(35.0, sCentralParkBoundingBox.lonEast))
        mEastPolyline.setPoints(list)
        mMapView!!.getOverlays()!!.add(mEastPolyline)

        mMapView!!.controller!!.setZoom(13.0)

        setHasOptionsMenu(true)

        mMapView!!.post(object : Runnable {
            // "post" because we need View.getWidth() to be set
            override fun run() {
                setLimitScrollingLatitude(true)
                setLimitScrollingLongitude(true)
            }
        })
    }

    /**
     * @since 6.0.0
     */
    private fun setLimitScrollingLatitude(pLimitScrolling: Boolean) {
        mMapView!!.getOverlays()!!.remove(mNorthPolyline)
        mMapView!!.getOverlays()!!.remove(mSouthPolyline)
        if (pLimitScrolling) {
            mMapView!!.setScrollableAreaLimitLatitude(
                sCentralParkBoundingBox.actualNorth,
                sCentralParkBoundingBox.actualSouth,
                mMapView!!.getHeight() / 2
            )
            mMapView!!.setExpectedCenter(sCentralParkBoundingBox.centerWithDateLine)
            mMapView!!.getOverlays()!!.add(mNorthPolyline)
            mMapView!!.getOverlays()!!.add(mSouthPolyline)
        } else {
            mMapView!!.resetScrollableAreaLimitLatitude()
        }
        mMapView!!.invalidate()
    }

    /**
     * @since 6.0.0
     */
    private fun setLimitScrollingLongitude(pLimitScrolling: Boolean) {
        mMapView!!.getOverlays()!!.remove(mWestPolyline)
        mMapView!!.getOverlays()!!.remove(mEastPolyline)
        if (pLimitScrolling) {
            mMapView!!.setScrollableAreaLimitLongitude(
                sCentralParkBoundingBox.lonWest,
                sCentralParkBoundingBox.lonEast,
                mMapView!!.getWidth() / 2
            )
            mMapView!!.setExpectedCenter(sCentralParkBoundingBox.centerWithDateLine)
            mMapView!!.getOverlays()!!.add(mWestPolyline)
            mMapView!!.getOverlays()!!.add(mEastPolyline)
        } else {
            mMapView!!.resetScrollableAreaLimitLongitude()
        }
        mMapView!!.invalidate()
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_LIMIT_SCROLLING_LAT_ID, Menu.NONE, "Latitude: Limit scrolling").setCheckable(true)
        menu.add(0, MENU_LIMIT_SCROLLING_LNG_ID, Menu.NONE, "Longitude: Limit scrolling").setCheckable(true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(MENU_LIMIT_SCROLLING_LAT_ID).setChecked(mMapView!!.isScrollableAreaLimitLatitude())
        menu.findItem(MENU_LIMIT_SCROLLING_LNG_ID).setChecked(mMapView!!.isScrollableAreaLimitLongitude())
        super.onPrepareOptionsMenu(menu)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            MENU_LIMIT_SCROLLING_LAT_ID -> {
                setLimitScrollingLatitude(!mMapView!!.isScrollableAreaLimitLatitude())
                return true
            }

            MENU_LIMIT_SCROLLING_LNG_ID -> {
                setLimitScrollingLongitude(!mMapView!!.isScrollableAreaLimitLongitude())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private const val SAMPLE_TITLE: String = "Limited scroll area"
    }
}

// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid.samplefragments.data

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

/**
 * Generates a ton of icons on the map, for scale testing
 *
 * @author Nicolas Gramlich
 */
class SampleWithMinimapItemizedOverlayWithScale : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    // ===========================================================
    // Constructors
    // ===========================================================
    override fun addOverlays() {
        super.addOverlays()

        val context: Context = requireActivity()

        /* Itemized Overlay */
        val iconOverlay: ItemizedOverlayWithFocus<OverlayItem?>
        run {
            /* Create a static ItemizedOverlay showing some Markers on various cities. */
            val items = ArrayList<OverlayItem?>()
            for (i in 0..4999) {
                val random_lon = getTileSystem().getRandomLongitude(Math.random())
                val random_lat = getTileSystem().getRandomLatitude(Math.random())
                items.add(
                    OverlayItem(
                        "A random point", "SampleDescription", GeoPoint(
                            random_lat,
                            random_lon
                        )
                    )
                )
            }
            items.add(
                OverlayItem(
                    "Berlin", "This is a relatively short SampleDescription.",
                    GeoPoint(52.518333, 13.408333)
                )
            ) // Berlin
            items.add(
                OverlayItem(
                    "Washington",
                    "This SampleDescription is a pretty long one. Almost as long as a the great wall in china.",
                    GeoPoint(38.895000, -77.036667)
                )
            ) // Washington
            items.add(
                OverlayItem(
                    "San Francisco", "SampleDescription", GeoPoint(
                        37.779300,
                        -122.419200
                    )
                )
            ) // San Francisco

            /* OnTapListener for the Markers, shows a simple Toast. */
            iconOverlay = ItemizedOverlayWithFocus<OverlayItem?>(
                items,
                object : OnItemGestureListener<OverlayItem?> {
                    override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                        Toast.makeText(
                            context,
                            ("Item '" + item!!.getTitle() + "' (index=" + index
                                    + ") got single tapped up"), Toast.LENGTH_LONG
                        ).show()
                        return true
                    }

                    override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                        Toast.makeText(
                            context,
                            ("Item '" + item!!.getTitle() + "' (index=" + index
                                    + ") got long pressed"), Toast.LENGTH_LONG
                        ).show()
                        return true
                    }
                }, context
            )
            iconOverlay.setFocusItemsOnTap(true)
            iconOverlay.setFocusedItem(0)

            mMapView!!.getOverlays()!!.add(iconOverlay)

            val mRotationGestureOverlay: RotationGestureOverlay
            mRotationGestureOverlay = RotationGestureOverlay(mMapView)
            mRotationGestureOverlay.setEnabled(false)
            mMapView!!.getOverlays()!!.add(mRotationGestureOverlay)
        }

        val rotationGestureOverlay = RotationGestureOverlay(mMapView)
        rotationGestureOverlay.setEnabled(true)
        mMapView!!.getOverlays()!!.add(rotationGestureOverlay)

        // Zoom and center on the focused item.
        mMapView!!.controller!!.setZoom(5.0)
        val geoPoint = iconOverlay.focusedItem!!.getPoint()
        mMapView!!.controller!!.animateTo(geoPoint)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Put overlay items first
        mMapView!!.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView)

        menu.add(0, MENU_ZOOMIN_ID, Menu.NONE, "ZoomIn")
        menu.add(0, MENU_ZOOMOUT_ID, Menu.NONE, "ZoomOut")

        super.onCreateOptionsMenu(menu, inflater)
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        mMapView!!.getOverlayManager().onPrepareOptionsMenu(menu, MENU_LAST_ID, mMapView)
        super.onPrepareOptionsMenu(menu)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMapView!!.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView)) return true

        when (item.getItemId()) {
            MENU_ZOOMIN_ID -> {
                mMapView!!.controller!!.zoomIn()
                return true
            }

            MENU_ZOOMOUT_ID -> {
                mMapView!!.controller!!.zoomOut()
                return true
            }
        }
        return false
    } // ===========================================================
    // Methods
    // ===========================================================
    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private const val SAMPLE_TITLE: String = "Itemized overlay at Scale"
            // ===========================================================

        private val MENU_ZOOMIN_ID = Menu.FIRST
        private val MENU_ZOOMOUT_ID: Int = MENU_ZOOMIN_ID + 1
        private val MENU_LAST_ID: Int = MENU_ZOOMOUT_ID + 1 // Always set to last unused id
    }
}

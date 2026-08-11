// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid.samplefragments.data

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.MinimapOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

/**
 * @author Nicolas Gramlich
 */
class SampleWithMinimapItemizedOverlayWithFocus : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * Called when the activity is first created.
     */
    public override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun addOverlays() {
        super.addOverlays()

        val context: Context? = getActivity()

        /* Itemized Overlay */
        val mMyLocationOverlay: ItemizedOverlayWithFocus<OverlayItem?>
        run {
            /* Create a static ItemizedOverlay showing some Markers on various cities. */
            val items = ArrayList<OverlayItem?>()
            items.add(
                OverlayItem(
                    "Hannover", "Tiny SampleDescription", GeoPoint(
                        52.370816,
                        9.735936
                    )
                )
            ) // Hannover
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
                    GeoPoint(38.895, -77.036667)
                )
            ) // Washington
            items.add(
                OverlayItem(
                    "San Francisco", "SampleDescription", GeoPoint(
                        37.7793,
                        -122.4192
                    )
                )
            ) // San Francisco

            /* OnTapListener for the Markers, shows a simple Toast. */
            mMyLocationOverlay = ItemizedOverlayWithFocus<OverlayItem?>(
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
            mMyLocationOverlay.setFocusItemsOnTap(true)
            mMyLocationOverlay.setFocusedItem(0)
            //https://github.com/osmdroid/osmdroid/issues/317
            //you can override the drawing characteristics with this
            mMyLocationOverlay.setMarkerBackgroundColor(Color.BLUE)
            mMyLocationOverlay.setMarkerTitleForegroundColor(Color.WHITE)
            mMyLocationOverlay.setMarkerDescriptionForegroundColor(Color.WHITE)
            mMyLocationOverlay.setDescriptionBoxPadding(15)

            mMapView!!.getOverlays()!!.add(mMyLocationOverlay)

            val mRotationGestureOverlay: RotationGestureOverlay
            mRotationGestureOverlay = RotationGestureOverlay(mMapView)
            mRotationGestureOverlay.setEnabled(false)
            mMapView!!.getOverlays()!!.add(mRotationGestureOverlay)
        }

        /* MiniMap */
        run {
            val miniMapOverlay = MinimapOverlay(
                context,
                mMapView!!.getTileRequestCompleteHandler()
            )
            mMapView!!.getOverlays()!!.add(miniMapOverlay)
        }

        // Zoom and center on the focused item.
        mMapView!!.controller!!.setZoom(5.0)
        val geoPoint = mMyLocationOverlay.getFocusedItem()!!.getPoint()
        mMapView!!.controller!!.animateTo(geoPoint)

        setHasOptionsMenu(true)
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
        private const val SAMPLE_TITLE: String = "Itemized overlay w/focus"
            // ===========================================================

        private val MENU_ZOOMIN_ID = Menu.FIRST
        private val MENU_ZOOMOUT_ID: Int = MENU_ZOOMIN_ID + 1
        private val MENU_LAST_ID: Int = MENU_ZOOMOUT_ID + 1 // Always set to last unused id
    }
}

// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid.samples

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.MinimapOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

/**
 * @author Nicolas Gramlich
 */
class SampleWithMinimapItemizedoverlay : AppCompatActivity() {
    private var mMapView: MapView? = null
    private var mMyLocationOverlay: ItemizedOverlay<OverlayItem?>? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_samplewithminimapitemizedoverlay)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        val mapContainer = findViewById<LinearLayout>(R.id.map_container)

        this.mMapView = MapView(this)
        this.mMapView!!.setTilesScaledToDpi(true)
        mapContainer.addView(
            this.mMapView, RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        )

        //Copyright overlay
        val copyrightNotice = mMapView!!.getTileProvider()!!.getTileSource().copyrightNotice
        val copyrightOverlay = CopyrightOverlay(this)
        copyrightOverlay.setCopyrightNotice(copyrightNotice)
        mMapView!!.getOverlays()!!.add(copyrightOverlay)

        /* Itemized Overlay */
        run {
            /* Create a static ItemizedOverlay showing a some Markers on some cities. */
            val items = ArrayList<OverlayItem?>()
            items.add(
                OverlayItem(
                    "Hannover", "SampleDescription",
                    GeoPoint(52.370816, 9.735936)
                )
            )
            items.add(
                OverlayItem(
                    "Berlin", "SampleDescription",
                    GeoPoint(52.518333, 13.408333)
                )
            )
            items.add(
                OverlayItem(
                    "Washington", "SampleDescription",
                    GeoPoint(38.895000, -77.036667)
                )
            )
            items.add(
                OverlayItem(
                    "San Francisco", "SampleDescription",
                    GeoPoint(37.779300, -122.419200)
                )
            )
            items.add(
                OverlayItem(
                    "Tolaga Bay", "SampleDescription",
                    GeoPoint(-38.371000, 178.298000)
                )
            )

            /* OnTapListener for the Markers, shows a simple Toast. */
            this.mMyLocationOverlay = ItemizedIconOverlay<OverlayItem?>(
                items,
                object : OnItemGestureListener<OverlayItem?> {
                    override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                        Toast.makeText(
                            this@SampleWithMinimapItemizedoverlay,
                            ("Item '" + item?.getTitle() + "' (index=" + index
                                    + ") got single tapped up"), Toast.LENGTH_LONG
                        ).show()
                        return true // We 'handled' this event.
                    }

                    override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                        Toast.makeText(
                            this@SampleWithMinimapItemizedoverlay,
                            ("Item '" + item?.getTitle() + "' (index=" + index
                                    + ") got long pressed"), Toast.LENGTH_LONG
                        ).show()
                        return true
                    }
                }, getApplicationContext()
            )
            this.mMapView!!.getOverlays()!!.add(this.mMyLocationOverlay)
        }

        /* MiniMap */
        run {
            val miniMapOverlay = MinimapOverlay(
                this,
                mMapView!!.getTileRequestCompleteHandler()
            )
            this.mMapView!!.getOverlays()!!.add(miniMapOverlay)
        }

        /* list of items currently displayed */
        run {
            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    return false
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    val displayed = mMyLocationOverlay!!.getDisplayedItems()
                    val buffer = StringBuilder()
                    var sep = ""
                    for (item in displayed) {
                        buffer.append(sep).append('\'').append(item?.getTitle()).append('\'')
                        sep = ", "
                    }
                    Toast.makeText(
                        this@SampleWithMinimapItemizedoverlay,
                        "Currently displayed: " + buffer.toString(), Toast.LENGTH_LONG
                    ).show()
                    return true
                }
            }
            mMapView!!.getOverlays()!!.add(MapEventsOverlay(mReceive))

            val rotationGestureOverlay = RotationGestureOverlay(mMapView)
            rotationGestureOverlay.setEnabled(true)
            mMapView!!.getOverlays()!!.add(rotationGestureOverlay)
        }

        // Default location and zoom level
        val mapController = mMapView!!.controller
        mapController!!.setZoom(5.0)
        val startPoint = GeoPoint(50.936255, 6.957779)
        mapController.setCenter(startPoint)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    public override fun onPause() {
        super.onPause()
        mMapView!!.onPause()
    }

    public override fun onResume() {
        super.onResume()
        mMapView!!.onResume()
    }
}

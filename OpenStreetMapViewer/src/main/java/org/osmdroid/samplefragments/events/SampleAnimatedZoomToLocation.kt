package org.osmdroid.samplefragments.events

import android.content.Context
import android.location.Location
import android.widget.Toast
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.MinimapOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

/**
 * @author Tyrone Tudehope
 */
class SampleAnimatedZoomToLocation : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    private var mMyLocationOverlay: ItemizedOverlayWithFocus<OverlayItem?>? = null
    private var mRotationGestureOverlay: RotationGestureOverlay? = null
    private var mGpsMyLocationProvider: GpsMyLocationProvider? = null

    public override fun onPause() {
        super.onPause()
        if (mGpsMyLocationProvider != null) {
            mGpsMyLocationProvider!!.stopLocationProvider()
        }
    }

    override fun addOverlays() {
        super.addOverlays()

        val context: Context? = getActivity()
        Toast.makeText(getActivity(), "Make sure location services are enabled!", Toast.LENGTH_LONG).show()
        mGpsMyLocationProvider = GpsMyLocationProvider(context)
        mGpsMyLocationProvider!!.startLocationProvider(object : IMyLocationConsumer {
            override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
                mGpsMyLocationProvider!!.stopLocationProvider()
                if (mMyLocationOverlay == null) {
                    val items = ArrayList<OverlayItem?>()
                    items.add(
                        OverlayItem(
                            "Me", "My Location",
                            GeoPoint(requireNotNull(location))
                        )
                    )

                    mMyLocationOverlay = ItemizedOverlayWithFocus<OverlayItem?>(
                        items,
                        object : OnItemGestureListener<OverlayItem?> {
                            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                                val mapController = mMapView!!.controller
                                mapController!!.setCenter(requireNotNull(item).getPoint())
                                mapController.zoomTo(mMapView!!.maxZoomLevel)
                                return true
                            }

                            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                                return false
                            }
                        }, context
                    )

                    mMyLocationOverlay!!.setFocusItemsOnTap(true)
                    mMyLocationOverlay!!.setFocusedItem(0)

                    mMapView!!.getOverlays()!!.add(mMyLocationOverlay)

                    mMapView!!.controller!!.setZoom(10)
                    val geoPoint = mMyLocationOverlay!!.getFocusedItem()!!.getPoint()
                    mMapView!!.controller!!.animateTo(geoPoint)
                }
            }
        })

        mRotationGestureOverlay = RotationGestureOverlay(mMapView)
        mRotationGestureOverlay!!.setEnabled(false)
        mMapView!!.getOverlays()!!.add(mRotationGestureOverlay)

        val miniMapOverlay = MinimapOverlay(
            context,
            mMapView!!.getTileRequestCompleteHandler()
        )
        mMapView!!.getOverlays()!!.add(miniMapOverlay)
    }

    companion object {
        private const val SAMPLE_TITLE: String = "Animated Zoom to Location"
    }
}

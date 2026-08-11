package org.osmdroid.samplefragments.tilesources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Offline First and Offline Second demos
 * The typical difference is when you pan the map to places you've never been to.
 * In the Offline First demo, you'll see an approximation of the tile before the actual downloaded
 * In the Offline Second demo, you'll see a gray square before the actual downloaded
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
abstract class SampleOfflinePriority : BaseSampleFragment() {
    private val mInitialCenter = GeoPoint(41.8905495, 12.4924348) // Rome, Italy
    private val mInitialZoomLevel = 5.0

    protected abstract val isOfflineFirst: Boolean

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val provider = MapTileProviderBasic(getActivity())
        provider.setOfflineFirst(this.isOfflineFirst)
        mMapView = MapView(inflater.getContext(), provider)
        return mMapView
    }

    override fun addOverlays() {
        super.addOverlays()

        mMapView!!.post(object : Runnable {
            // "post" because we need View.getWidth() to be set
            override fun run() {
                mMapView!!.controller!!.setZoom(mInitialZoomLevel)
                mMapView!!.setExpectedCenter(mInitialCenter)
            }
        })
    }

    override val sampleTitle: String
        get() = "Offline " + (if (this.isOfflineFirst) "First" else "Second")
}

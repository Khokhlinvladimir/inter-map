package org.osmdroid.bugtestfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * created on 5/30/2022.
 *
 * @author seadowg
 */
class Bug1783MyLocationOverlayNPE : DialogFragment() {
    private var myLocationNewOverlay: MyLocationNewOverlay? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return MapView(getContext()!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapView = view as MapView
        myLocationNewOverlay = MyLocationNewOverlay(mapView)
        mapView.getOverlays()!!.add(myLocationNewOverlay)
    }


    override fun onPause() {
        super.onPause()
        myLocationNewOverlay!!.disableFollowLocation()
    }
}

package org.osmdroid.views.overlay

import android.view.Menu
import android.view.MenuItem
import org.osmdroid.views.MapView

interface IOverlayMenuProvider {
    fun onCreateOptionsMenu(pMenu: Menu?, pMenuIdOffset: Int,
                            pMapView: MapView?): Boolean

    fun onPrepareOptionsMenu(pMenu: Menu?, pMenuIdOffset: Int,
                             pMapView: MapView?): Boolean

    fun onOptionsItemSelected(pItem: MenuItem?, pMenuIdOffset: Int,
                              pMapView: MapView?): Boolean

    /**
     * Can be used to signal to external callers that this Overlay should not be used for providing
     * option menu items.
     */
    var isOptionsMenuEnabled: Boolean
}
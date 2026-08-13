package org.osmdroid.views.drawing

import android.os.Handler
import android.os.Message
import org.osmdroid.tileprovider.MapTileProviderBase

class MapSnapshotHandler(private var mMapSnapshot: MapSnapshot?) : Handler() {
    override fun handleMessage(msg: Message) {
        if (msg.what == MapTileProviderBase.Companion.MAPTILE_SUCCESS_ID) {
            val mapSnapshot = mMapSnapshot
            if (mapSnapshot != null) { // in case it was destroyed just before
                mapSnapshot.refreshASAP()
            }
        }
    }

    fun destroy() {
        mMapSnapshot = null
    }
}

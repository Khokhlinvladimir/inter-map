package org.osmdroid.tileprovider.util

import android.os.Handler
import android.os.Message
import android.view.View
import org.osmdroid.tileprovider.MapTileProviderBase

class SimpleInvalidationHandler(pView: View?) : Handler() {
    private var mView: View?

    init {
        mView = pView
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MapTileProviderBase.Companion.MAPTILE_SUCCESS_ID -> if (mView != null) mView!!.invalidate()
        }
    }

    /**
     * See [https://github.com/osmdroid/osmdroid/issues/390](https://github.com/osmdroid/osmdroid/issues/390)
     */
    fun destroy() {
        mView = null
    }
}

package org.osmdroid.tileprovider.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.osmdroid.tileprovider.IRegisterReceiver

abstract class MapTileFileStorageProviderBase(
    pRegisterReceiver: IRegisterReceiver,
    pThreadPoolSize: Int, pPendingQueueSize: Int
) : MapTileModuleProviderBase(pThreadPoolSize, pPendingQueueSize) {
    private val mRegisterReceiver: IRegisterReceiver
    private var mBroadcastReceiver: MyBroadcastReceiver?

    init {
        mRegisterReceiver = pRegisterReceiver
        mBroadcastReceiver = MyBroadcastReceiver()

        val mediaFilter = IntentFilter()
        mediaFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        mediaFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        mediaFilter.addDataScheme("file")
        pRegisterReceiver.registerReceiver(mBroadcastReceiver, mediaFilter)
    }

    override fun detach() {
        if (mBroadcastReceiver != null) {
            mRegisterReceiver.unregisterReceiver(mBroadcastReceiver)
            mBroadcastReceiver = null
        }
        super.detach()
    }

    protected open fun onMediaMounted() {
        // Do nothing by default. Override to handle.
    }

    protected open fun onMediaUnmounted() {
        // Do nothing by default. Override to handle.
    }

    /**
     * This broadcast receiver will recheck the sd card when the mount/unmount messages happen
     */
    private inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(aContext: Context?, aIntent: Intent) {
            val action = aIntent.getAction()

            if (Intent.ACTION_MEDIA_MOUNTED == action) {
                onMediaMounted()
            } else if (Intent.ACTION_MEDIA_UNMOUNTED == action) {
                onMediaUnmounted()
            }
        }
    }
}

package org.osmdroid.tileprovider.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.osmdroid.tileprovider.IRegisterReceiver

class SimpleRegisterReceiver(pContext: Context?) : IRegisterReceiver {
    private var mContext: Context?

    init {
        mContext = pContext
    }

    override fun registerReceiver(aReceiver: BroadcastReceiver?, aFilter: IntentFilter?): Intent? {
        return mContext!!.registerReceiver(aReceiver, aFilter)
    }

    override fun unregisterReceiver(aReceiver: BroadcastReceiver?) {
        mContext!!.unregisterReceiver(aReceiver)
    }

    override fun destroy() {
        mContext = null
    }
}

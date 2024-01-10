package org.osmdroid.tileprovider

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter

interface IRegisterReceiver {
    fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent?
    fun unregisterReceiver(receiver: BroadcastReceiver?)
    fun destroy()
}
package org.osmdroid.tileprovider.modules

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build

/**
 * A straightforward network check implementation.
 *
 * @author Marc Kurtz
 */
class NetworkAvailabliltyCheck(aContext: Context) : INetworkAvailablityCheck {
    private val mConnectionManager: ConnectivityManager
    private val mIsX86: Boolean
    private val mHasNetworkStatePermission: Boolean

    init {
        mConnectionManager = aContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        mIsX86 = "Android-x86".equals(Build.BRAND, ignoreCase = true)

        mHasNetworkStatePermission = (aContext.getPackageManager()
            .checkPermission(Manifest.permission.ACCESS_NETWORK_STATE, aContext.getPackageName())
                == PackageManager.PERMISSION_GRANTED)
    }

    override val networkAvailable: Boolean
        get() {
            if (!mHasNetworkStatePermission) {
                // if we're unable to check network state, assume we have a network
                return true
            }
            val networkInfo = mConnectionManager.getActiveNetworkInfo()
            if (networkInfo == null) {
                return false
            }
            if (networkInfo.isConnected()) {
                return true
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2) return mIsX86 && networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET
            return false
        }

    override val wiFiNetworkAvailable: Boolean
        get() {
            if (!mHasNetworkStatePermission) {
                // if we're unable to check network state, assume we have a network
                return true
            }
            val wifi = mConnectionManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            return wifi != null && wifi.isConnected()
        }

    override val cellularDataNetworkAvailable: Boolean
        get() {
            if (!mHasNetworkStatePermission) {
                // if we're unable to check network state, assume we have a network
                return true
            }
            val mobile = mConnectionManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            return mobile != null && mobile.isConnected()
        }

    @Deprecated("")
    override fun getRouteToPathExists(hostAddress: Int): Boolean {
        // TODO check for CHANGE_NETWORK_STATE permission
        //return mConnectionManager.requestRouteToHost(ConnectivityManager.TYPE_WIFI, hostAddress)
        //	|| mConnectionManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE, hostAddress);
        return true
    }
}

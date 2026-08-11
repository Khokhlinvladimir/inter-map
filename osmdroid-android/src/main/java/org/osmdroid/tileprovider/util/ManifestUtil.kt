package org.osmdroid.tileprovider.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import org.osmdroid.api.IMapView

/**
 * Utility class for reading the manifest
 */
object ManifestUtil {
    /**
     * Retrieve a key from the manifest meta data, or empty string if not found.
     */
    fun retrieveKey(aContext: Context, aKey: String?): String {
        // get the key from the manifest

        val pm = aContext.getPackageManager()
        try {
            val info = pm.getApplicationInfo(
                aContext.getPackageName(),
                PackageManager.GET_META_DATA
            )
            if (info.metaData == null) {
                Log.i(IMapView.LOGTAG, "Key %s not found in manifest" + aKey)
            } else {
                val value = info.metaData.getString(aKey)
                if (value == null) {
                    Log.i(IMapView.LOGTAG, "Key %s not found in manifest" + aKey)
                } else {
                    return value.trim { it <= ' ' }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.i(IMapView.LOGTAG, "Key %s not found in manifest" + aKey)
        }
        return ""
    }
}

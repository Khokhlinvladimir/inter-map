package org.osmdroid.tileprovider.util

import android.util.Log

/**
 * The counters class is a simple container for tracking various internal statistics for osmdroid,
 * useful for troubleshooting osmdroid, finding memory leaks and more
 * Created by alex on 6/16/16.
 */
object Counters {
    const val TAG: String = "osmCounters"

    /**
     * out of memory errors
     */
    var countOOM: Int = 0

    var tileDownloadErrors: Int = 0

    var fileCacheSaveErrors: Int = 0

    var fileCacheMiss: Int = 0

    var fileCacheOOM: Int = 0
    var fileCacheHit: Int = 0

    /**
     * @since 6.2.0
     */
    private val sMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()

    fun printToLogcat() {
        Log.d(TAG, "countOOM " + countOOM)
        Log.d(TAG, "tileDownloadErrors " + tileDownloadErrors)
        Log.d(TAG, "fileCacheSaveErrors " + fileCacheSaveErrors)
        Log.d(TAG, "fileCacheMiss " + fileCacheMiss)
        Log.d(TAG, "fileCacheOOM " + fileCacheOOM)
        Log.d(TAG, "fileCacheHit " + fileCacheHit)
    }

    fun reset() {
        countOOM = 0
        tileDownloadErrors = 0
        fileCacheSaveErrors = 0
        fileCacheMiss = 0
        fileCacheOOM = 0
        fileCacheHit = 0
    }

    /**
     * @since 6.2.0
     */
    fun reset(pTag: String?) {
        sMap.remove(pTag)
    }

    /**
     * @since 6.2.0
     */
    fun increment(pTag: String?) {
        val value = sMap.get(pTag)
        if (value == null) {
            sMap.put(pTag, 1)
        } else {
            sMap.put(pTag, value + 1)
        }
    }

    /**
     * @since 6.2.0
     */
    fun get(pTag: String?): Int {
        val value = sMap.get(pTag)
        if (value == null) {
            return 0
        } else {
            return value
        }
    }
}

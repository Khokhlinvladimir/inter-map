package org.osmdroid.config

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.util.StorageUtils
import java.io.File
import java.net.Proxy
import java.text.SimpleDateFormat
import java.util.Locale
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.HTTP_EXPIRES_HEADER_FORMAT

/**
 * Default configuration provider for osmdroid
 * [Issue 481](https://github.com/osmdroid/osmdroid/issues/481)
 * Created on 11/29/2016.
 *
 * @author Alex O'Ree
 * @see IConfigurationProvider
 *
 * @see Configuration
 *
 * @since 5.6
 */
class DefaultConfigurationProvider : IConfigurationProvider {
    /**
     * default is 20 seconds
     *
     * @return time in ms
     */
    override var gpsWaitTime: Long = 20000
    override var isDebugMode: Boolean = false
    override var isDebugMapView: Boolean = false
    override var isDebugTileProviders: Boolean = false
    override var isDebugMapTileDownloader: Boolean = false
    override var isMapViewHardwareAccelerated: Boolean = true
    override var userAgentValue: String? = DEFAULT_USER_AGENT
    override var userAgentHttpHeader: String? = "User-Agent"
    override val additionalHttpRequestProperties: MutableMap<String?, String?> = HashMap<String?, String?>()
    override var cacheMapTileCount: Short = 9
    override var tileDownloadThreads: Short = 2
    override var tileFileSystemThreads: Short = 8
    override var tileDownloadMaxQueueSize: Short = 40
    override var tileFileSystemMaxQueueSize: Short = 40
    override var tileFileSystemCacheMaxBytes: Long = 600L * 1024 * 1024
    override var tileFileSystemCacheTrimBytes: Long = 500L * 1024 * 1024
    override var httpHeaderDateTimeFormat: SimpleDateFormat? = SimpleDateFormat(HTTP_EXPIRES_HEADER_FORMAT, Locale.US)
    private var basePath: File? = null
    override var osmdroidBasePath: File?
        get() = getOsmdroidBasePath(null)
        set(value) {
            basePath = value
        }
    private var tileCache: File? = null
    override var osmdroidTileCache: File?
        get() = getOsmdroidTileCache(null)
        set(value) {
            tileCache = value
        }
    protected var expirationAdder: Long = 0
    override var expirationOverrideDuration: Long? = null
    override var httpProxy: Proxy? = null
    override var animationSpeedDefault: Int = 1000
    override var animationSpeedShort: Int = 500
    override var isMapViewRecyclerFriendly: Boolean = true
    override var cacheMapTileOvershoot: Short = 0
    override var tileGCFrequencyInMillis: Long = 300000
    override var tileGCBulkSize: Int = 20
    override var tileGCBulkPauseInMillis: Long = 500
    override var isMapTileDownloaderFollowRedirects: Boolean = true
    override var isEnforceTileSystemBounds: Boolean = false

    /**
     * @since 6.1.0
     */
    /**
     * @since 6.1.0
     */
    override var normalizedUserAgent: String? = null
        private set

    override fun getOsmdroidBasePath(context: Context?): File? {
        try {
            if (basePath == null) {
                val storageInfo = StorageUtils.getBestWritableStorage(context)
                if (storageInfo != null) {
                    val pathToStorage = storageInfo.path
                    basePath = File(pathToStorage, "osmdroid")
                    basePath!!.mkdirs()
                } else {
                    var fallbackBasePath: File? = null
                    // FIXME NOT SUPPORTED VERSION FROYO
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                        fallbackBasePath = File(
                            context!!.getExternalFilesDir(
                                Environment.DIRECTORY_PICTURES
                            ), "osmdroid"
                        )
                    }
                    if (!fallbackBasePath!!.mkdirs()) {
                        Log.e(IMapView.LOGTAG, "Directory not created")
                    }
                    basePath = fallbackBasePath
                }
            }
        } catch (ex: Exception) {
            Log.d(IMapView.LOGTAG, "Unable to create base path at " + basePath, ex)
            //IO/permissions issue
            //trap for android studio layout editor and some for certain devices
            //see https://github.com/osmdroid/osmdroid/issues/508
        }
        if (basePath == null && context != null) basePath = context.filesDir
        return basePath
    }

    override fun getOsmdroidTileCache(context: Context?): File? {
        if (tileCache == null) tileCache = File(getOsmdroidBasePath(context), "tiles")
        try {
            tileCache!!.mkdirs()
        } catch (ex: Exception) {
            Log.d(IMapView.LOGTAG, "Unable to create tile cache path at " + tileCache, ex)
            //IO/permissions issue
            //trap for android studio layout editor and some for certain devices
            //see https://github.com/osmdroid/osmdroid/issues/508
        }
        return tileCache
    }

    //</editor-fold>
    override fun load(ctx: Context, prefs: SharedPreferences) {
        this.normalizedUserAgent = computeNormalizedUserAgent(ctx)

        //cache management starts here

        //check to see if the shared preferences is set for the tile cache
        val basePathStr = prefs.getString("osmdroid.basePath", null)
        if (basePathStr == null || !File(basePathStr).exists()) {
            //this is the first time startup. run the discovery bit
            var discoveredBasePath = getOsmdroidBasePath(ctx)
            var discoveredCachePath = getOsmdroidTileCache(ctx)
            if (!discoveredBasePath!!.exists() || !StorageUtils.isWritable(discoveredBasePath)) {
                //this should always be writable...
                discoveredBasePath = File(ctx.getFilesDir(), "osmdroid")
                discoveredCachePath = File(discoveredBasePath, "tiles")
                discoveredCachePath.mkdirs()
            }

            val edit = prefs.edit()
            edit.putString("osmdroid.basePath", discoveredBasePath.getAbsolutePath())
            edit.putString("osmdroid.cachePath", discoveredCachePath!!.getAbsolutePath())
            commit(edit)
            osmdroidBasePath = discoveredBasePath
            osmdroidTileCache = discoveredCachePath
            userAgentValue = ctx.getPackageName()
            save(ctx, prefs)
        } else {
            //normal startup, load user preferences and populate the config object
            osmdroidBasePath = File(prefs.getString("osmdroid.basePath", getOsmdroidBasePath(ctx)!!.getAbsolutePath()))
            osmdroidTileCache = File(prefs.getString("osmdroid.cachePath", getOsmdroidTileCache(ctx)!!.getAbsolutePath()))
            isDebugMode = prefs.getBoolean("osmdroid.DebugMode", this.isDebugMode)
            isDebugMapTileDownloader = prefs.getBoolean("osmdroid.DebugDownloading", this.isDebugMapTileDownloader)
            isDebugMapView = prefs.getBoolean("osmdroid.DebugMapView", this.isDebugMapView)
            isDebugTileProviders = prefs.getBoolean("osmdroid.DebugTileProvider", this.isDebugTileProviders)
            isMapViewHardwareAccelerated = prefs.getBoolean("osmdroid.HardwareAcceleration", isMapViewHardwareAccelerated)
            userAgentValue = prefs.getString("osmdroid.userAgentValue", ctx.getPackageName())
            load(prefs, this.additionalHttpRequestProperties, "osmdroid.additionalHttpRequestProperty.")
            gpsWaitTime = prefs.getLong("osmdroid.gpsWaitTime", gpsWaitTime)
            tileDownloadThreads = (prefs.getInt("osmdroid.tileDownloadThreads", tileDownloadThreads.toInt())).toShort()
            tileFileSystemThreads = (prefs.getInt("osmdroid.tileFileSystemThreads", tileFileSystemThreads.toInt())).toShort()
            tileDownloadMaxQueueSize = (prefs.getInt("osmdroid.tileDownloadMaxQueueSize", tileDownloadMaxQueueSize.toInt())).toShort()
            tileFileSystemMaxQueueSize = (prefs.getInt("osmdroid.tileFileSystemMaxQueueSize", tileFileSystemMaxQueueSize.toInt())).toShort()
            expirationExtendedDuration = prefs.getLong("osmdroid.ExpirationExtendedDuration", expirationAdder)
            isMapViewRecyclerFriendly = prefs.getBoolean("osmdroid.mapViewRecycler", this.isMapViewRecyclerFriendly)
            animationSpeedDefault = prefs.getInt("osmdroid.ZoomSpeedDefault", animationSpeedDefault)
            animationSpeedShort = prefs.getInt("osmdroid.animationSpeedShort", animationSpeedShort)
            cacheMapTileOvershoot = (prefs.getInt("osmdroid.cacheTileOvershoot", cacheMapTileOvershoot.toInt())).toShort()
            isMapTileDownloaderFollowRedirects = prefs.getBoolean("osmdroid.TileDownloaderFollowRedirects", this.isMapTileDownloaderFollowRedirects)
            isEnforceTileSystemBounds = prefs.getBoolean("osmdroid.enforceTileSystemBounds", false)
            if (prefs.contains("osmdroid.ExpirationOverride")) {
                this.expirationOverrideDuration = prefs.getLong("osmdroid.ExpirationOverride", -1)
                if (this.expirationOverrideDuration != null && this.expirationOverrideDuration == -1L) this.expirationOverrideDuration = null
            }
        }

        if (Build.VERSION.SDK_INT >= 9) {
            //unfortunately API 8 doesn't support File.length()

            //https://github/osmdroid/osmdroid/issues/435
            //On startup, we auto set the max cache size to be the current cache size + free disk space
            //this reduces the chance of osmdroid completely filling up the storage device

            //if the default max cache size is greater than the available free space
            //reduce it to 95% of the available free space + the size of the cache

            var cacheSize: Long = 0
            val dbFile = File(osmdroidTileCache!!.getAbsolutePath() + File.separator + SqlTileWriter.Companion.DATABASE_FILENAME)
            if (dbFile.exists()) {
                cacheSize = dbFile.length()
            }

            val freeSpace = osmdroidTileCache!!.getFreeSpace()

            //Log.i(TAG, "Current cache size is " + cacheSize + " free space is " + freeSpace);
            if (tileFileSystemCacheMaxBytes > (freeSpace + cacheSize)) {
                tileFileSystemCacheMaxBytes = ((freeSpace + cacheSize) * 0.95).toLong()
                tileFileSystemCacheTrimBytes = ((freeSpace + cacheSize) * 0.90).toLong()
            }
        }
    }

    override fun save(ctx: Context?, prefs: SharedPreferences) {
        val edit = prefs.edit()
        edit.putString("osmdroid.basePath", osmdroidBasePath!!.getAbsolutePath())
        edit.putString("osmdroid.cachePath", osmdroidTileCache!!.getAbsolutePath())
        edit.putBoolean("osmdroid.DebugMode", isDebugMode)
        edit.putBoolean("osmdroid.DebugDownloading", isDebugMapTileDownloader)
        edit.putBoolean("osmdroid.DebugMapView", isDebugMapView)
        edit.putBoolean("osmdroid.DebugTileProvider", isDebugTileProviders)
        edit.putBoolean("osmdroid.HardwareAcceleration", isMapViewHardwareAccelerated)
        edit.putBoolean("osmdroid.TileDownloaderFollowRedirects", isMapTileDownloaderFollowRedirects)
        edit.putString("osmdroid.userAgentValue", userAgentValue)
        save(prefs, edit, this.additionalHttpRequestProperties, "osmdroid.additionalHttpRequestProperty.")
        edit.putLong("osmdroid.gpsWaitTime", gpsWaitTime)
        edit.putInt("osmdroid.cacheMapTileCount", cacheMapTileCount.toInt())
        edit.putInt("osmdroid.tileDownloadThreads", tileDownloadThreads.toInt())
        edit.putInt("osmdroid.tileFileSystemThreads", tileFileSystemThreads.toInt())
        edit.putInt("osmdroid.tileDownloadMaxQueueSize", tileDownloadMaxQueueSize.toInt())
        edit.putInt("osmdroid.tileFileSystemMaxQueueSize", tileFileSystemMaxQueueSize.toInt())
        edit.putLong("osmdroid.ExpirationExtendedDuration", expirationAdder)
        if (this.expirationOverrideDuration != null) edit.putLong("osmdroid.ExpirationOverride", this.expirationOverrideDuration!!)
        //TODO save other fields?
        edit.putInt("osmdroid.ZoomSpeedDefault", animationSpeedDefault)
        edit.putInt("osmdroid.animationSpeedShort", animationSpeedShort)
        edit.putBoolean("osmdroid.mapViewRecycler", this.isMapViewRecyclerFriendly)
        edit.putInt("osmdroid.cacheTileOvershoot", cacheMapTileOvershoot.toInt())
        edit.putBoolean("osmdroid.enforceTileSystemBounds", this.isEnforceTileSystemBounds)
        commit(edit)
    }

    override var expirationExtendedDuration: Long
        get() = expirationAdder
        set(period) {
            if (period < 0) expirationAdder = 0
            else expirationAdder = period
        }

    /**
     * @since 6.1.0
     */
    private fun computeNormalizedUserAgent(pContext: Context?): String? {
        if (pContext == null) {
            return null
        }
        val packageName = pContext.getPackageName()
        try {
            val packageInfo = pContext.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val version = packageInfo.versionCode
            return packageName + "/" + version
        } catch (e1: PackageManager.NameNotFoundException) {
            return packageName
        }
    }

    companion object {
        const val DEFAULT_USER_AGENT: String = "osmdroid"

        /**
         * Loading a map from preferences, using a prefix for the prefs keys
         *
         * @param pPrefs
         * @param pMap
         * @param pPrefix
         * @since 5.6.5
         */
        private fun load(
            pPrefs: SharedPreferences,
            pMap: MutableMap<String?, String?>?, pPrefix: String?
        ) {
            //potential fix for #1079   https://github.com/osmdroid/osmdroid/issues/1079
            if (pPrefix == null || pMap == null) return
            pMap.clear()

            for (key in pPrefs.getAll().keys) {
                if (key != null && key.startsWith(pPrefix)) {
                    pMap.put(key.substring(pPrefix.length), pPrefs.getString(key, null))
                }
            }
        }

        /**
         * Saving a map into preferences, using a prefix for the prefs keys
         *
         * @param pPrefs
         * @param pEdit
         * @param pMap
         * @param pPrefix
         * @since 5.6.5
         */
        private fun save(
            pPrefs: SharedPreferences, pEdit: SharedPreferences.Editor,
            pMap: MutableMap<String?, String?>, pPrefix: String
        ) {
            for (key in pPrefs.getAll().keys) {
                if (key.startsWith(pPrefix)) {
                    pEdit.remove(key)
                }
            }
            for (entry in pMap.entries) {
                val key = pPrefix + entry.key
                pEdit.putString(key, entry.value)
            }
        }

        private fun commit(pEditor: SharedPreferences.Editor) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                pEditor.apply()
            } else {
                pEditor.commit()
            }
        }
    }
}

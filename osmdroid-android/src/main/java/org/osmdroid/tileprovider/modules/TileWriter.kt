package org.osmdroid.tileprovider.modules

import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.ExpirableBitmapDrawable
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.tileprovider.util.StreamUtils
import org.osmdroid.util.MapTileIndex
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Arrays

/**
 * An implementation of [IFilesystemCache]. It writes tiles to the file system cache. If the
 * cache exceeds 600 Mb then it will be trimmed to 500 Mb.
 *
 * @author Neil Boyd
 * @see OpenStreetMapTileProviderConstants
 */
class TileWriter : IFilesystemCache {
    var initThread: Thread? = null
    private var mMaximumCachedFileAge: Long = 0

    // ===========================================================
    // Constructors
    // ===========================================================
    init {
        if (!hasInited) {
            hasInited = true
            // do this in the background because it takes a long time
            initThread = object : Thread() {
                override fun run() {
                    usedCacheSpace = 0 // because it's static

                    calculateDirectorySize(instance!!.osmdroidTileCache!!)

                    if (usedCacheSpace > instance!!.tileFileSystemCacheMaxBytes) {
                        cutCurrentCache()
                    }
                    if (instance!!.isDebugMode) {
                        Log.d(IMapView.LOGTAG, "Finished init thread")
                    }
                }
            }
            initThread!!.setName("TileWriter#init")
            initThread!!.setPriority(Thread.MIN_PRIORITY)
            initThread!!.start()
        }
    }

    fun setMaximumCachedFileAge(mMaximumCachedFileAge: Long) {
        this.mMaximumCachedFileAge = mMaximumCachedFileAge
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun saveFile(
        pTileSource: ITileSource?, pMapTileIndex: Long,
        pStream: InputStream?, pExpirationTime: Long?
    ): Boolean {
        if (pTileSource == null || pStream == null) return false
        val file = getFile(pTileSource, pMapTileIndex)

        if (instance!!.isDebugTileProviders) {
            Log.d(IMapView.LOGTAG, "TileWrite " + file.getAbsolutePath())
        }
        val parent = file.getParentFile()
        if (!parent!!.exists() && !createFolderAndCheckIfExists(parent)) {
            return false
        }

        var outputStream: BufferedOutputStream? = null
        try {
            outputStream = BufferedOutputStream(
                FileOutputStream(file.getPath()),
                StreamUtils.IO_BUFFER_SIZE
            )
            val length = StreamUtils.copy(pStream, outputStream)

            usedCacheSpace += length
            if (usedCacheSpace > instance!!.tileFileSystemCacheMaxBytes) {
                cutCurrentCache() // TODO perhaps we should do this in the background
            }
        } catch (e: IOException) {
            Counters.fileCacheSaveErrors++
            return false
        } finally {
            if (outputStream != null) {
                StreamUtils.closeStream(outputStream)
            }
        }
        return true
    }

    override fun onDetach() {
        if (initThread != null) {
            try {
                initThread!!.interrupt()
            } catch (t: Throwable) {
            }
        }
    }

    override fun remove(pTileSource: ITileSource?, pMapTileIndex: Long): Boolean {
        if (pTileSource == null) return false
        val file = getFile(pTileSource, pMapTileIndex)

        if (file.exists()) {
            try {
                return file.delete()
            } catch (ex: Exception) {
                //potential io exception
                Log.i(IMapView.LOGTAG, "Unable to delete cached tile from " + pTileSource.name() + " " + MapTileIndex.toString(pMapTileIndex), ex)
            }
        }
        return false
    }

    /**
     * @since 5.6.5
     */
    fun getFile(pTileSource: ITileSource, pMapTileIndex: Long): File {
        return File(
            instance!!.osmdroidTileCache, pTileSource.getTileRelativeFilenameString(pMapTileIndex)
                    + OpenStreetMapTileProviderConstants.TILE_PATH_EXTENSION
        )
    }

    override fun exists(pTileSource: ITileSource?, pMapTileIndex: Long): Boolean {
        if (pTileSource == null) return false
        return getFile(pTileSource, pMapTileIndex).exists()
    }

    // ===========================================================
    // Methods
    // ===========================================================
    private fun createFolderAndCheckIfExists(pFile: File): Boolean {
        if (pFile.mkdirs()) {
            return true
        }
        if (instance!!.isDebugMode) {
            Log.d(IMapView.LOGTAG, "Failed to create " + pFile + " - wait and check again")
        }

        // if create failed, wait a bit in case another thread created it
        try {
            Thread.sleep(500)
        } catch (ignore: InterruptedException) {
        }
        // and then check again
        if (pFile.exists()) {
            if (instance!!.isDebugMode) {
                Log.d(IMapView.LOGTAG, "Seems like another thread created " + pFile)
            }
            return true
        } else {
            if (instance!!.isDebugMode) {
                Log.d(IMapView.LOGTAG, "File still doesn't exist: " + pFile)
            }
            return false
        }
    }

    private fun calculateDirectorySize(pDirectory: File) {
        val z = pDirectory.listFiles()
        if (z != null) {
            for (file in z) {
                if (file.isFile()) {
                    usedCacheSpace += file.length()
                }
                if (file.isDirectory() && !isSymbolicDirectoryLink(pDirectory, file)) {
                    calculateDirectorySize(file) // *** recurse ***
                }
            }
        }
    }

    /**
     * Checks to see if it appears that a directory is a symbolic link. It does this by comparing
     * the canonical path of the parent directory and the parent directory of the directory's
     * canonical path. If they are equal, then they come from the same true parent. If not, then
     * pDirectory is a symbolic link. If we get an exception, we err on the side of caution and
     * return "true" expecting the calculateDirectorySize to now skip further processing since
     * something went goofy.
     */
    private fun isSymbolicDirectoryLink(pParentDirectory: File, pDirectory: File): Boolean {
        try {
            val canonicalParentPath1 = pParentDirectory.getCanonicalPath()
            val canonicalParentPath2 = pDirectory.getCanonicalFile().getParent()
            return canonicalParentPath1 != canonicalParentPath2
        } catch (e: IOException) {
            return true
        } catch (e: NoSuchElementException) {
            // See: http://code.google.com/p/android/issues/detail?id=4961
            // See: http://code.google.com/p/android/issues/detail?id=5807
            return true
        }
    }

    private fun getDirectoryFileList(aDirectory: File): MutableList<File?> {
        val files: MutableList<File?> = ArrayList<File?>()

        val z = aDirectory.listFiles()
        if (z != null) {
            for (file in z) {
                if (file.isFile()) {
                    files.add(file)
                }
                if (file.isDirectory()) {
                    files.addAll(getDirectoryFileList(file))
                }
            }
        }

        return files
    }

    /**
     * If the cache size is greater than the max then trim it down to the trim level. This method is
     * synchronized so that only one thread can run it at a time.
     */
    private fun cutCurrentCache() {
        val lock = instance!!.osmdroidTileCache
        synchronized(lock!!) {
            if (usedCacheSpace > instance!!.tileFileSystemCacheTrimBytes) {
                Log.d(
                    IMapView.LOGTAG, ("Trimming tile cache from " + usedCacheSpace + " to "
                            + instance!!.tileFileSystemCacheTrimBytes)
                )

                val z = getDirectoryFileList(instance!!.osmdroidTileCache!!)

                // order list by files day created from old to new
                val files: Array<File> = z.filterNotNull().toTypedArray()
                files.sortBy { it.lastModified() }

                for (file in files) {
                    if (usedCacheSpace <= instance!!.tileFileSystemCacheTrimBytes) {
                        break
                    }

                    val length = file.length()
                    if (file.delete()) {
                        if (instance!!.isDebugTileProviders) {
                            Log.d(IMapView.LOGTAG, "Cache trim deleting " + file.getAbsolutePath())
                        }
                        usedCacheSpace -= length
                    }
                }

                Log.d(IMapView.LOGTAG, "Finished trimming tile cache")
            }
        }
    }

    override fun getExpirationTimestamp(pTileSource: ITileSource?, pMapTileIndex: Long): Long? {
        return null
    }

    @Throws(Exception::class)
    override fun loadTile(pTileSource: ITileSource?, pMapTileIndex: Long): Drawable? {
        if (pTileSource == null) return null
        // Check the tile source to see if its file is available and if so, then render the
        // drawable and return the tile
        val file = getFile(pTileSource, pMapTileIndex)
        if (!file.exists()) {
            return null
        }

        val drawable = pTileSource.getDrawable(file.getPath())

        // Check to see if file has expired
        val now = System.currentTimeMillis()
        val lastModified = file.lastModified()
        val fileExpired = lastModified < now - mMaximumCachedFileAge

        if (fileExpired && drawable != null) {
            if (instance!!.isDebugMode) {
                Log.d(IMapView.LOGTAG, "Tile expired: " + MapTileIndex.toString(pMapTileIndex))
            }
            ExpirableBitmapDrawable.Companion.setState(drawable, ExpirableBitmapDrawable.Companion.EXPIRED)
        }

        return drawable
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        // ===========================================================
        // Fields
        // ===========================================================
        /**
         * Get the amount of disk space used by the tile cache. This will initially be zero since the
         * used space is calculated in the background.
         *
         * @return size in bytes
         */
        /**
         * amount of disk space used by tile cache
         */
        var usedCacheSpace: Long = 0
            private set
        var hasInited: Boolean = false
        // ===========================================================
        // Getter & Setter
        // ===========================================================
    }
}

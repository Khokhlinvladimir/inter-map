// Created by plusminus on 21:46:41 - 25.09.2008
package org.osmdroid.tileprovider.modules

import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.util.StreamUtils
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.TileSystem
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * A tile provider that can serve tiles from an archive using the supplied tile source. The tile
 * provider will automatically find existing archives and use each one that it finds.
 *
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 */
class MapTileFileArchiveProvider(
    pRegisterReceiver: IRegisterReceiver,
    pTileSource: ITileSource?, pArchives: Array<out IArchiveFile?>?, ignoreTileSource: Boolean
) : MapTileFileStorageProviderBase(
    pRegisterReceiver,
    instance!!.tileFileSystemThreads.toInt(),
    instance!!.tileFileSystemMaxQueueSize.toInt()
) {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    private val mArchiveFiles = ArrayList<IArchiveFile?>()

    private val mTileSource = AtomicReference<ITileSource?>()

    /**
     * Disable the search of archives if specified in constructor
     */
    private val mSpecificArchivesProvided: Boolean
    private val ignoreTileSource: Boolean

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * The tiles may be found on several media. This one works with tiles stored on the file system.
     * It and its friends are typically created and controlled by [MapTileProviderBase].
     */
    @JvmOverloads
    constructor(
        pRegisterReceiver: IRegisterReceiver,
        pTileSource: ITileSource?, pArchives: Array<out IArchiveFile?>? = null
    ) : this(pRegisterReceiver, pTileSource, pArchives, false)

    /**
     * @param pRegisterReceiver
     * @param pTileSource
     * @param pArchives
     * @param ignoreTileSource  if true, tile source is ignored
     * @since 6.0.0
     */
    init {
        this.ignoreTileSource = ignoreTileSource
        setTileSource(pTileSource)

        if (pArchives == null) {
            mSpecificArchivesProvided = false
            findArchiveFiles()
        } else {
            mSpecificArchivesProvided = true
            for (i in pArchives.indices.reversed()) {
                mArchiveFiles.add(pArchives[i])
            }
        }
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun getUsesDataConnection(): Boolean {
        return false
    }

    override fun getName(): String {
        return "File Archive Provider"
    }

    override fun getThreadGroupName(): String {
        return "filearchive"
    }

    override fun getTileLoader(): MapTileModuleProviderBase.TileLoader {
        return TileLoader()
    }

    override fun getMinimumZoomLevel(): Int {
        val tileSource = mTileSource.get()
        return if (tileSource != null) tileSource.minimumZoomLevel else OpenStreetMapTileProviderConstants.MINIMUM_ZOOM_LEVEL
    }

    override fun getMaximumZoomLevel(): Int {
        val tileSource = mTileSource.get()
        return if (tileSource != null)
            tileSource.maximumZoomLevel
        else
            TileSystem.maximumZoomLevel
    }

    override fun onMediaMounted() {
        if (!mSpecificArchivesProvided) {
            findArchiveFiles()
        }
    }

    override fun onMediaUnmounted() {
        if (!mSpecificArchivesProvided) {
            findArchiveFiles()
        }
    }

    override fun setTileSource(pTileSource: ITileSource?) {
        mTileSource.set(pTileSource)
    }

    override fun detach() {
        clearArcives()
        super.detach()
    }

    private fun clearArcives() {
        while (!mArchiveFiles.isEmpty()) {
            val t = mArchiveFiles.get(0)
            if (t != null) t.close()
            mArchiveFiles.removeAt(0)
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================
    private fun findArchiveFiles() {
        clearArcives()

        // path should be optionally configurable
        val cachePaths = instance!!.osmdroidBasePath
        if (cachePaths != null) {
            val files = cachePaths.listFiles()
            if (files != null) {
                for (file in files) {
                    val archiveFile = ArchiveFileFactory.getArchiveFile(file)
                    if (archiveFile != null) {
                        archiveFile.setIgnoreTileSource(ignoreTileSource)
                        mArchiveFiles.add(archiveFile)
                    }
                }
            }
        }
    }

    @Synchronized
    private fun getInputStream(
        pMapTileIndex: Long,
        tileSource: ITileSource?
    ): InputStream? {
        for (archiveFile in mArchiveFiles) {
            if (archiveFile != null) {
                val `in` = archiveFile.getInputStream(tileSource, pMapTileIndex)
                if (`in` != null) {
                    if (instance!!.isDebugMode) {
                        Log.d(IMapView.LOGTAG, "Found tile " + MapTileIndex.toString(pMapTileIndex) + " in " + archiveFile)
                    }
                    return `in`
                }
            }
        }

        return null
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    protected inner class TileLoader : MapTileModuleProviderBase.TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            var returnValue: Drawable? = null
            val tileSource = mTileSource.get()
            if (tileSource == null) {
                return null
            }

            var inputStream: InputStream? = null
            try {
                if (instance!!.isDebugMode) {
                    Log.d(IMapView.LOGTAG, "Archives - Tile doesn't exist: " + MapTileIndex.toString(pMapTileIndex))
                }

                inputStream = getInputStream(pMapTileIndex, tileSource)
                if (inputStream != null) {
                    if (instance!!.isDebugMode) {
                        Log.d(IMapView.LOGTAG, "Use tile from archive: " + MapTileIndex.toString(pMapTileIndex))
                    }
                    val drawable = tileSource.getDrawable(inputStream)
                    returnValue = drawable
                }
            } catch (e: Throwable) {
                Log.e(IMapView.LOGTAG, "Error loading tile", e)
            } finally {
                if (inputStream != null) {
                    StreamUtils.closeStream(inputStream)
                }
            }

            return returnValue
        }
    }
}

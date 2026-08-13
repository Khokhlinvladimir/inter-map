package org.osmdroid.tileprovider.modules

import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ZipFileArchive : IArchiveFile {
    protected var mZipFile: ZipFile? = null
    private var mIgnoreTileSource = false

    constructor()

    private constructor(pZipFile: ZipFile) {
        mZipFile = pZipFile
    }

    /**
     * @since 6.0
     * If set to true, tiles from this archive will be loaded regardless of their associated tile source name
     */
    override fun setIgnoreTileSource(pIgnoreTileSource: Boolean) {
        mIgnoreTileSource = pIgnoreTileSource
    }

    @Throws(Exception::class)
    override fun init(pFile: File?) {
        mZipFile = ZipFile(pFile)
    }

    override fun getInputStream(pTileSource: ITileSource?, pMapTileIndex: Long): InputStream? {
        try {
            if (!mIgnoreTileSource && pTileSource != null) {
                val path = pTileSource.getTileRelativeFilenameString(pMapTileIndex)
                val entry = mZipFile!!.getEntry(path)
                if (entry != null) {
                    return mZipFile!!.getInputStream(entry)
                }
            } else {
                // Search all sources in ZIP internal order
                val entries = mZipFile!!.entries()
                while (entries.hasMoreElements()) {
                    val nextElement: ZipEntry = entries.nextElement()
                    val str = nextElement.name
                    if (str.contains("/")) {
                        val path =
                            getTileRelativeFilenameString(pMapTileIndex, str.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
                        val entry = mZipFile!!.getEntry(path)
                        if (entry != null) {
                            return mZipFile!!.getInputStream(entry)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.w(IMapView.LOGTAG, "Error getting zip stream: " + MapTileIndex.toString(pMapTileIndex), e)
        }
        return null
    }

    /**
     * @since 6.0
     * Creating paths for ZIP scanning
     */
    private fun getTileRelativeFilenameString(pMapTileIndex: Long, pathBase: String?): String {
        val sb = StringBuilder()
        sb.append(pathBase)
        sb.append('/')
        sb.append(MapTileIndex.getZoom(pMapTileIndex))
        sb.append('/')
        sb.append(MapTileIndex.getX(pMapTileIndex))
        sb.append('/')
        sb.append(MapTileIndex.getY(pMapTileIndex))
        sb.append(".png")
        return sb.toString()
    }

    override val tileSources: MutableSet<String?>
        get() {
            val ret: MutableSet<String?> = HashSet<String?>()
            try {
                val entries = mZipFile!!.entries()
                while (entries.hasMoreElements()) {
                    val nextElement: ZipEntry = entries.nextElement()
                    val str = nextElement.name
                    if (str.contains("/")) ret.add(str.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
                }
            } catch (e: Exception) {
                Log.w(IMapView.LOGTAG, "Error getting tile sources: ", e)
            }
            return ret
        }

    override fun close() {
        try {
            mZipFile!!.close()
        } catch (e: IOException) {
        }
    }

    override fun toString(): String {
        return "ZipFileArchive [mZipFile=" + mZipFile!!.name + "]"
    }

    companion object {
        @Throws(ZipException::class, IOException::class)
        fun getZipFileArchive(pFile: File?): ZipFileArchive {
            return ZipFileArchive(ZipFile(pFile))
        }
    }
}

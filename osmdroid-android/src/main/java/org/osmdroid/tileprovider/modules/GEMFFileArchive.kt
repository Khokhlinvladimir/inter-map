package org.osmdroid.tileprovider.modules

import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.GEMFFile
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.io.IOException
import java.io.InputStream

class GEMFFileArchive : IArchiveFile {
    private var mFile: GEMFFile? = null

    constructor()

    private constructor(pFile: File) {
        mFile = GEMFFile(pFile)
    }

    @Throws(Exception::class)
    override fun init(pFile: File?) {
        if (pFile == null) return
        mFile = GEMFFile(pFile)
    }

    override fun getInputStream(pTileSource: ITileSource?, pMapTileIndex: Long): InputStream? {
        return mFile!!.getInputStream(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex))
    }


    override val tileSources: MutableSet<String?>
        get() {
            val ret: MutableSet<String?> = HashSet<String?>()
            try {
                ret.addAll(mFile!!.sources.values)
            } catch (e: Exception) {
                Log.w(IMapView.LOGTAG, "Error getting tile sources: ", e)
            }
            return ret
        }

    override fun setIgnoreTileSource(pIgnoreTileSource: Boolean) {
    }

    override fun close() {
        try {
            mFile!!.close()
        } catch (e: IOException) {
        }
    }

    override fun toString(): String {
        return "GEMFFileArchive [mGEMFFile=" + mFile!!.name + "]"
    }

    companion object {
        @Throws(IOException::class)
        fun getGEMFFileArchive(pFile: File): GEMFFileArchive {
            return GEMFFileArchive(pFile)
        }
    }
}

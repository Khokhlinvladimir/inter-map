package org.osmdroid.tileprovider.modules

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.Collections
import kotlin.math.pow

/**
 * supports raster imagery in the MBTiles 1.1 spec
 * https://sourceforge.net/p/mobac/code/HEAD/tree/trunk/MOBAC/src/main/java/mobac/program/atlascreators/MBTiles.java
 * https://github.com/mapbox/mbtiles-spec/tree/master/1.1
 *
 * @author neilboyd circa 2011
 */
class MBTilesFileArchive : IArchiveFile {
    private var mDatabase: SQLiteDatabase? = null

    constructor()

    private constructor(pDatabase: SQLiteDatabase) {
        mDatabase = pDatabase
    }

    @Throws(Exception::class)
    override fun init(pFile: File?) {
        if (pFile == null) return
        mDatabase = SQLiteDatabase.openDatabase(
            pFile.getAbsolutePath(),
            null,
            SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READONLY
        )
    }

    override fun getInputStream(pTileSource: ITileSource?, pMapTileIndex: Long): InputStream? {
        try {
            var ret: InputStream? = null
            val tile = arrayOf<String?>(COL_TILES_TILE_DATA)
            val xyz = arrayOf<String?>(
                MapTileIndex.getX(pMapTileIndex).toString(),
                (2.0.pow(
                    MapTileIndex.getZoom(pMapTileIndex).toDouble()
                ) - MapTileIndex.getY(pMapTileIndex) - 1).toString(),  // Use Google Tiling Spec
                MapTileIndex.getZoom(pMapTileIndex).toString()
            )

            val cur = mDatabase!!.query(TABLE_TILES, tile, "tile_column=? and tile_row=? and zoom_level=?", xyz, null, null, null)

            if (cur.getCount() != 0) {
                cur.moveToFirst()
                ret = ByteArrayInputStream(cur.getBlob(0))
            }
            cur.close()
            if (ret != null) {
                return ret
            }
        } catch (e: Throwable) {
            Log.w(IMapView.LOGTAG, "Error getting db stream: " + MapTileIndex.toString(pMapTileIndex), e)
        }

        return null
    }

    override val tileSources: MutableSet<String?>
        get() =//the MBTiles spec doesn't store source information in it, so we can't return anything
            Collections.emptySet<String?>().toMutableSet()

    override fun setIgnoreTileSource(pIgnoreTileSource: Boolean) {
    }

    override fun close() {
        mDatabase!!.close()
    }

    override fun toString(): String {
        return "DatabaseFileArchive [mDatabase=" + mDatabase!!.getPath() + "]"
    }

    companion object {
        //	TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
        const val TABLE_TILES: String = "tiles"
        const val COL_TILES_ZOOM_LEVEL: String = "zoom_level"
        const val COL_TILES_TILE_COLUMN: String = "tile_column"
        const val COL_TILES_TILE_ROW: String = "tile_row"
        const val COL_TILES_TILE_DATA: String = "tile_data"

        @Throws(SQLiteException::class)
        fun getDatabaseFileArchive(pFile: File): MBTilesFileArchive {
            return MBTilesFileArchive(
                SQLiteDatabase.openDatabase(
                    pFile.getAbsolutePath(),
                    null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READONLY
                )
            )
        }
    }
}

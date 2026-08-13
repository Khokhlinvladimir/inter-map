package org.osmdroid.tileprovider.modules

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * This is the OSMdroid style database provider. It's an extremely simply sqlite database schema.
 * CREATE TABLE tiles (key INTEGER PRIMARY KEY, provider TEXT, tile BLOB)
 * where the key is the X/Y/Z coordinates bitshifted using the following algorithm
 * key = ((z &lt;&lt; z) + x &lt;&lt; z) + y;
 *
 * @see SqlTileWriter
 */
class DatabaseFileArchive : IArchiveFile {
    private var mDatabase: SQLiteDatabase? = null
    private var mIgnoreTileSource = false

    constructor()

    private constructor(pDatabase: SQLiteDatabase?) {
        mDatabase = pDatabase
    }

    /**
     * @since 6.0
     * If set to true, tiles from this archive will be loaded regardless of their associated tile source name
     */
    override fun setIgnoreTileSource(pIgnoreTileSource: Boolean) {
        mIgnoreTileSource = pIgnoreTileSource
    }

    override val tileSources: MutableSet<String?>
        get() {
            val ret: MutableSet<String?> = HashSet<String?>()
            try {
                val cur = mDatabase!!.rawQuery("SELECT distinct provider FROM " + TABLE, null)
                while (cur.moveToNext()) {
                    ret.add(cur.getString(0))
                }
                cur.close()
            } catch (e: Exception) {
                Log.w(IMapView.LOGTAG, "Error getting tile sources: ", e)
            }
            return ret
        }

    @Throws(Exception::class)
    override fun init(pFile: File?) {
        if (pFile == null) return
        mDatabase = SQLiteDatabase.openDatabase(pFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
    }

    fun getImage(pTileSource: ITileSource?, pMapTileIndex: Long): ByteArray? {
        if (pTileSource == null) return null
        if (mDatabase == null || !mDatabase!!.isOpen()) {
            if (instance!!.isDebugTileProviders) Log.d(IMapView.LOGTAG, "Skipping DatabaseFileArchive lookup, database is closed")
            return null
        }
        try {
            var bits: ByteArray? = null
            val tile = arrayOf<String?>(COLUMN_TILE)
            val x = MapTileIndex.getX(pMapTileIndex).toLong()
            val y = MapTileIndex.getY(pMapTileIndex).toLong()
            val z = MapTileIndex.getZoom(pMapTileIndex).toLong()
            val index = ((z shl z.toInt()) + x shl z.toInt()) + y

            val cur: Cursor
            if (!mIgnoreTileSource) {
                cur = mDatabase!!.query(
                    TABLE, tile, (COLUMN_KEY + " = " + index + " and "
                            + COLUMN_PROVIDER + " = ?"), arrayOf<String?>(pTileSource.name()), null, null, null
                )
            } else {
                cur = mDatabase!!.query(TABLE, tile, COLUMN_KEY + " = " + index, null, null, null, null)
            }

            if (cur.getCount() != 0) {
                cur.moveToFirst()
                bits = (cur.getBlob(0))
            }
            cur.close()
            if (bits != null) {
                return bits
            }
        } catch (e: Throwable) {
            Log.w(IMapView.LOGTAG, "Error getting db stream: " + MapTileIndex.toString(pMapTileIndex), e)
        }

        return null
    }

    override fun getInputStream(pTileSource: ITileSource?, pMapTileIndex: Long): InputStream? {
        try {
            var ret: InputStream? = null
            val bits = getImage(pTileSource, pMapTileIndex)
            if (bits != null) ret = ByteArrayInputStream(bits)
            if (ret != null) {
                return ret
            }
        } catch (e: Throwable) {
            Log.w(IMapView.LOGTAG, "Error getting db stream: " + MapTileIndex.toString(pMapTileIndex), e)
        }
        return null
    }

    override fun close() {
        mDatabase!!.close()
    }

    override fun toString(): String {
        return "DatabaseFileArchive [mDatabase=" + mDatabase!!.getPath() + "]"
    }

    companion object {
        const val TABLE: String = "tiles"
        const val COLUMN_PROVIDER: String = "provider"
        const val COLUMN_TILE: String = "tile"
        const val COLUMN_KEY: String = "key"
        val tile_column: Array<String?> = arrayOf<String?>("tile")

        @Throws(SQLiteException::class)
        fun getDatabaseFileArchive(pFile: File): DatabaseFileArchive {
            return DatabaseFileArchive(
                SQLiteDatabase.openDatabase(
                    pFile.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
                )
            )
        }
    }
}

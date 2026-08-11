package org.osmdroid.tileprovider.modules

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.util.StreamUtils
import org.osmdroid.util.MapTileIndex
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * An implementation of [IFilesystemCache] based on the original TileWriter. It writes tiles to a sqlite database.
 * It does NOT support expiration and provides more of a MOBAC like functionality (non-expiring file archives).
 * Uses the same schema as MOBAC osm sqlite and the [DatabaseFileArchive]
 *
 *
 * https://github.com/osmdroid/osmdroid/issues/348
 *
 * @author Alex O'Ree
 * @see SqlTileWriter
 *
 * @see DatabaseFileArchive
 *
 * @since 5.2 7/8/16.
 */
class SqliteArchiveTileWriter(outputFile: String) : IFilesystemCache {
    val db_file: File
    val mDatabase: SQLiteDatabase?
    val questimate: Int = 8000
    override fun saveFile(pTileSourceInfo: ITileSource?, pMapTileIndex: Long, pStream: InputStream?, pExpirationTime: Long?): Boolean {
        if (pTileSourceInfo == null || pStream == null) return false
        if (mDatabase == null || !mDatabase.isOpen()) {
            Log.d(IMapView.LOGTAG, "Skipping SqlArchiveTileWriter saveFile, database is closed")
            return false
        }
        var returnValue = false
        var bos: ByteArrayOutputStream? = null
        try {
            val cv = ContentValues()
            val index: Long = SqlTileWriter.Companion.getIndex(pMapTileIndex)
            cv.put(DatabaseFileArchive.Companion.COLUMN_PROVIDER, pTileSourceInfo.name())

            val buffer = ByteArray(512)
            var l: Int
            bos = ByteArrayOutputStream()
            while ((pStream.read(buffer).also { l = it }) != -1) bos.write(buffer, 0, l)
            val bits = bos.toByteArray() // if a variable is required at all

            cv.put(DatabaseFileArchive.Companion.COLUMN_KEY, index)
            cv.put(DatabaseFileArchive.Companion.COLUMN_TILE, bits)
            mDatabase.insert(DatabaseFileArchive.Companion.TABLE, null, cv)
            returnValue = true
            if (instance!!.isDebugMode) Log.d(IMapView.LOGTAG, "tile inserted " + pTileSourceInfo.name() + MapTileIndex.toString(pMapTileIndex))
        } catch (ex: Throwable) {
            Log.e(IMapView.LOGTAG, "Unable to store cached tile from " + pTileSourceInfo.name() + " " + MapTileIndex.toString(pMapTileIndex), ex)
        } finally {
            try {
                bos!!.close()
            } catch (e: IOException) {
            }
        }
        return returnValue
    }


    override fun exists(pTileSource: ITileSource?, pMapTileIndex: Long): Boolean {
        if (pTileSource == null) return false
        try {
            val index: Long = SqlTileWriter.Companion.getIndex(pMapTileIndex)
            val cur = getTileCursor(SqlTileWriter.Companion.getPrimaryKeyParameters(index, pTileSource))
                ?: return false

            val result = (cur.getCount() != 0)
            cur.close()
            return result
        } catch (ex: Throwable) {
            Log.e(IMapView.LOGTAG, "Unable to store cached tile from " + pTileSource.name() + " " + MapTileIndex.toString(pMapTileIndex), ex)
        }
        return false
    }

    override fun onDetach() {
        if (mDatabase != null) mDatabase.close()
    }

    override fun remove(tileSource: ITileSource?, pMapTileIndex: Long): Boolean {
        //not supported
        return false
    }

    override fun getExpirationTimestamp(pTileSource: ITileSource?, pMapTileIndex: Long): Long? {
        return null
    }

    init {
        // do this in the background because it takes a long time
        db_file = File(outputFile)
        try {
            mDatabase = SQLiteDatabase.openOrCreateDatabase(db_file.getAbsolutePath(), null)
        } catch (ex: Exception) {
            throw Exception("Trouble creating database file at " + outputFile, ex)
        }
        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + DatabaseFileArchive.Companion.TABLE + " (" + DatabaseFileArchive.Companion.COLUMN_KEY + " INTEGER , " + DatabaseFileArchive.Companion.COLUMN_PROVIDER + " TEXT, tile BLOB, PRIMARY KEY (key, provider));")
        } catch (t: Throwable) {
            t.printStackTrace()
            Log.d(IMapView.LOGTAG, "error setting db schema, it probably exists already", t)
            // throw new IOException("Trouble creating database file"+ t.getMessage());
        }
    }

    /**
     * @param pPrimaryKeyParameters
     * @return
     * @since 5.6.5
     */
    fun getTileCursor(pPrimaryKeyParameters: Array<String?>?): Cursor? {
        if (mDatabase == null || !mDatabase.isOpen()) {
            Log.w(IMapView.LOGTAG, "Skipping SqlArchiveTileWriter getTileCursor, database is closed")
            return null
        }
        return mDatabase.query(
            DatabaseFileArchive.Companion.TABLE,
            queryColumns,
            SqlTileWriter.primaryKey,
            pPrimaryKeyParameters,
            null,
            null,
            null
        )
    }

    /**
     * @since 5.6.5
     */
    @Throws(Exception::class)
    override fun loadTile(pTileSource: ITileSource?, pMapTileIndex: Long): Drawable? {
        if (pTileSource == null) return null
        if (mDatabase == null || !mDatabase.isOpen()) {
            Log.w(IMapView.LOGTAG, "Skipping SqlArchiveTileWriter loadTile, database is closed")
            return null
        }
        var inputStream: InputStream? = null
        try {
            val index: Long = SqlTileWriter.Companion.getIndex(pMapTileIndex)
            val cur = getTileCursor(SqlTileWriter.Companion.getPrimaryKeyParameters(index, pTileSource))
            if (cur == null) return null
            var bits: ByteArray? = null

            if (cur.moveToFirst()) {
                bits = cur.getBlob(cur.getColumnIndex(DatabaseFileArchive.Companion.COLUMN_TILE))
            }
            cur.close()
            if (bits == null) {
                if (instance!!.isDebugMode) {
                    Log.d(IMapView.LOGTAG, "SqlCache - Tile doesn't exist: " + pTileSource.name() + MapTileIndex.toString(pMapTileIndex))
                }
                return null
            }
            inputStream = ByteArrayInputStream(bits)
            return pTileSource.getDrawable(inputStream)
        } finally {
            if (inputStream != null) {
                StreamUtils.closeStream(inputStream)
            }
        }
    }

    companion object {
        var hasInited: Boolean = false

        /**
         * For optimization reasons
         *
         * @since 5.6.5
         */
        private val queryColumns = arrayOf<String?>(DatabaseFileArchive.Companion.COLUMN_TILE)
    }
}

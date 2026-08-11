package org.osmdroid.tileprovider.modules

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.ExpirableBitmapDrawable
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.tileprovider.util.StreamUtils
import org.osmdroid.util.GarbageCollector
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.SplashScreenable
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * An implementation of [IFilesystemCache] based on the original TileWriter. It writes tiles to a sqlite database cache.
 * It supports expiration timestamps if provided by the server from which the tile was downloaded. Trimming
 * of expired
 *
 *
 * If the database exceeds [Configuration.getInstance]
 * cache exceeds 600 Mb then it will be trimmed to 500 Mb by deleting files that expire first.
 *
 * @author Alex O'Ree
 * @see DatabaseFileArchive
 *
 * @see SqliteArchiveTileWriter
 *
 * @since 5.1
 */
open class SqlTileWriter : IFilesystemCache, SplashScreenable {
    protected var lastSizeCheck: Long = 0
    private val garbageCollector = GarbageCollector(object : Runnable {
        override fun run() {
            runCleanupOperation()
        }
    })

    /**
     * this could be a long running operation, don't run on the UI thread unless necessary.
     * This function prunes the database for old or expired tiles.
     *
     * @since 5.6
     */
    fun runCleanupOperation() {
        val db = this.db
        if (db == null || !db.isOpen()) {
            if (instance!!.isDebugMode) {
                Log.d(IMapView.LOGTAG, "Finished init thread, aborted due to null database reference")
            }
            return
        }

        // index creation is run now (regardless of the table size)
        // therefore potentially on a small table, for better index creation performances
        createIndex(db)

        val dbLength: Long = db_file!!.length()
        if (dbLength <= instance!!.tileFileSystemCacheMaxBytes) {
            return
        }

        runCleanupOperation(
            dbLength - instance!!.tileFileSystemCacheTrimBytes,
            instance!!.tileGCBulkSize,
            instance!!.tileGCBulkPauseInMillis,
            true
        )
    }

    override fun saveFile(pTileSourceInfo: ITileSource?, pMapTileIndex: Long, pStream: InputStream?, pExpirationTime: Long?): Boolean {
        if (pTileSourceInfo == null || pStream == null) return false
        val db = this.db
        if (db == null || !db.isOpen()) {
            Log.d(
                IMapView.LOGTAG,
                "Unable to store cached tile from " + pTileSourceInfo.name() + " " + MapTileIndex.toString(pMapTileIndex) + ", database not available."
            )
            Counters.fileCacheSaveErrors++
            return false
        }
        var bos: ByteArrayOutputStream? = null
        try {
            val cv = ContentValues()
            val index: Long = getIndex(pMapTileIndex)
            cv.put(DatabaseFileArchive.Companion.COLUMN_PROVIDER, pTileSourceInfo.name())

            val buffer = ByteArray(512)
            var l: Int
            bos = ByteArrayOutputStream()
            while ((pStream.read(buffer).also { l = it }) != -1) bos.write(buffer, 0, l)
            val bits = bos.toByteArray() // if a variable is required at all

            cv.put(DatabaseFileArchive.Companion.COLUMN_KEY, index)
            cv.put(DatabaseFileArchive.Companion.COLUMN_TILE, bits)
            if (pExpirationTime != null) cv.put(COLUMN_EXPIRES, pExpirationTime)
            db.replaceOrThrow(DatabaseFileArchive.Companion.TABLE, null, cv)
            if (instance!!.isDebugMode) Log.d(IMapView.LOGTAG, "tile inserted " + pTileSourceInfo.name() + MapTileIndex.toString(pMapTileIndex))
            if (System.currentTimeMillis() > lastSizeCheck + instance!!.tileGCFrequencyInMillis) {
                lastSizeCheck = System.currentTimeMillis()
                garbageCollector.gc()
            }
        } catch (ex: SQLiteFullException) {
            //the drive is full! trigger the clean up operation
            //may want to consider reducing the trim size automagically
            Log.e(IMapView.LOGTAG, "SQLiteFullException while saving tile.", ex)
            garbageCollector.gc()
            catchException(ex)
        } catch (ex: Exception) {
            //note, although we check for db null state at the beginning of this method, it's possible for the
            //db to be closed during the execution of this method
            Log.e(
                IMapView.LOGTAG,
                "Unable to store cached tile from " + pTileSourceInfo.name() + " " + MapTileIndex.toString(pMapTileIndex) + " db is " + (if (db == null) "null" else "not null"),
                ex
            )
            Counters.fileCacheSaveErrors++
            catchException(ex)
        } finally {
            try {
                bos!!.close()
            } catch (e: IOException) {
            }
        }
        return false
    }

    /**
     * Returns true if the given tile source and tile coordinates exist in the cache
     *
     * @since 5.6
     */
    fun exists(pTileSource: String?, pMapTileIndex: Long): Boolean {
        return 1L == getRowCount(primaryKey, getPrimaryKeyParameters(getIndex(pMapTileIndex), pTileSource))
    }

    /**
     * Returns true if the given tile source and tile coordinates exist in the cache
     *
     * @since 5.6
     */
    override fun exists(pTileSource: ITileSource?, pMapTileIndex: Long): Boolean {
        if (pTileSource == null) return false
        return exists(pTileSource.name(), pMapTileIndex)
    }

    /**
     * Now we use only one static instance of database, which should never be closed
     */
    override fun onDetach() {
    }

    /**
     * purges and deletes everything from the cache database
     *
     * @return
     * @since 5.6
     */
    fun purgeCache(): Boolean {
        val db = this.db
        if (db != null && db.isOpen()) {
            try {
                db.delete(DatabaseFileArchive.Companion.TABLE, null, null)
                return true
            } catch (e: Exception) {
                Log.w(IMapView.LOGTAG, "Error purging the db", e)
                catchException(e)
            }
        }
        return false
    }

    /**
     * purges and deletes all tiles from the given tile source name from the cache database
     *
     * @return
     * @since 5.6.1
     */
    fun purgeCache(mTileSourceName: String?): Boolean {
        val db = this.db
        if (db != null && db.isOpen()) {
            try {
                db.delete(
                    DatabaseFileArchive.Companion.TABLE,
                    DatabaseFileArchive.Companion.COLUMN_PROVIDER + " = ?",
                    arrayOf<String?>(mTileSourceName)
                )
                return true
            } catch (e: Exception) {
                Log.w(IMapView.LOGTAG, "Error purging the db", e)
                catchException(e)
            }
        }
        return false
    }

    /**
     * a helper method to import file system stored map tiles into the sql tile cache
     * on successful import, the tiles are removed from the file system.
     *
     *
     * This can take a long time, so consider running this off of the main thread.
     *
     * @return
     */
    fun importFromFileCache(removeFromFileSystem: Boolean): IntArray {
        val db = this.db
        val ret = intArrayOf(0, 0, 0, 0)
        //inserts
        //insert failures
        //deletes
        //delete failures
        val tilePathBase = instance!!.osmdroidTileCache
        if (tilePathBase!!.exists()) {
            val tileSources = tilePathBase.listFiles()
            if (tileSources != null) {
                for (i in tileSources.indices) {
                    if (tileSources[i]!!.isDirectory() && !tileSources[i]!!.isHidden()) {
                        //proceed
                        val z = tileSources[i]!!.listFiles()
                        if (z != null) for (zz in z.indices) {
                            if (z[zz]!!.isDirectory() && !z[zz]!!.isHidden()) {
                                val x = z[zz]!!.listFiles()
                                if (x != null) for (xx in x.indices) {
                                    if (x[xx]!!.isDirectory() && !x[xx]!!.isHidden()) {
                                        val y = x[xx]!!.listFiles()
                                        if (x != null) for (yy in y!!.indices) {
                                            if (!y[yy]!!.isHidden() && !y[yy]!!.isDirectory()) {
                                                try {
                                                    val cv = ContentValues()
                                                    val x1 = x[xx]!!.name.toLong()
                                                    val y1 = y[yy]!!.name.substring(0, y[yy]!!.name.indexOf(".")).toLong()
                                                    val z1 = z[zz]!!.name.toLong()
                                                    val index: Long = getIndex(x1, y1, z1)
                                                    cv.put(DatabaseFileArchive.Companion.COLUMN_PROVIDER, tileSources[i]!!.name)
                                                    if (!exists(
                                                            tileSources[i]!!.name,
                                                            MapTileIndex.getTileIndex(z1.toInt(), x1.toInt(), y1.toInt())
                                                        )
                                                    ) {
                                                        val bis = BufferedInputStream(FileInputStream(y[yy]))

                                                        val list: MutableList<Byte?> = ArrayList<Byte?>()
                                                        //ByteArrayBuffer baf = new ByteArrayBuffer(500);
                                                        var current = 0
                                                        while ((bis.read().also { current = it }) != -1) {
                                                            list.add(current.toByte())
                                                        }

                                                        val bits = ByteArray(list.size)
                                                        for (bi in list.indices) {
                                                            bits[bi] = list.get(bi)!!
                                                        }
                                                        cv.put(DatabaseFileArchive.Companion.COLUMN_KEY, index)
                                                        cv.put(DatabaseFileArchive.Companion.COLUMN_TILE, bits)

                                                        val insert = db.insert(DatabaseFileArchive.Companion.TABLE, null, cv)
                                                        if (insert > 0) {
                                                            if (instance!!.isDebugMode) Log.d(
                                                                IMapView.LOGTAG,
                                                                "tile inserted " + tileSources[i]!!.name + "/" + z1 + "/" + x1 + "/" + y1
                                                            )
                                                            ret[0]++
                                                            if (removeFromFileSystem) {
                                                                try {
                                                                    y[yy]!!.delete()
                                                                    ret[2]++
                                                                } catch (ex: Exception) {
                                                                    ret[3]++
                                                                }
                                                            }
                                                        } else {
                                                            Log.w(
                                                                IMapView.LOGTAG,
                                                                "tile NOT inserted " + tileSources[i]!!.name + "/" + z1 + "/" + x1 + "/" + y1
                                                            )
                                                        }
                                                    }
                                                } catch (ex: Exception) {
                                                    //note, although we check for db null state at the beginning of this method, it's possible for the
                                                    //db to be closed during the execution of this method
                                                    Log.e(
                                                        IMapView.LOGTAG,
                                                        "Unable to store cached tile from " + tileSources[i]!!.name + " db is " + (if (db == null) "null" else "not null"),
                                                        ex
                                                    )
                                                    ret[1]++
                                                    catchException(ex)
                                                }
                                            }
                                        }
                                    }
                                    if (removeFromFileSystem) {
                                        //clean up the directories
                                        try {
                                            x[xx]!!.delete()
                                        } catch (ex: Exception) {
                                            Log.e(IMapView.LOGTAG, "Unable to delete directory from " + x[xx]!!.getAbsolutePath(), ex)
                                            ret[3]++
                                        }
                                    }
                                }
                            }
                            if (removeFromFileSystem) {
                                //clean up the directories
                                try {
                                    z[zz]!!.delete()
                                } catch (ex: Exception) {
                                    Log.e(IMapView.LOGTAG, "Unable to delete directory from " + z[zz]!!.getAbsolutePath(), ex)
                                    ret[3]++
                                }
                            }
                        }


                        if (removeFromFileSystem) {
                            //clean up the directories
                            try {
                                tileSources[i]!!.delete()
                            } catch (ex: Exception) {
                                Log.e(IMapView.LOGTAG, "Unable to delete directory from " + tileSources[i]!!.getAbsolutePath(), ex)
                                ret[3]++
                            }
                        }
                    } else {
                        //it's a file, nothing for us to do here
                    }
                }
            }
        }
        return ret
    }


    /**
     * Removes a specific tile from the cache
     *
     * @since 5.6
     */
    override fun remove(pTileSourceInfo: ITileSource?, pMapTileIndex: Long): Boolean {
        if (pTileSourceInfo == null) return false
        val db = this.db
        if (db == null || !db.isOpen()) {
            Log.d(
                IMapView.LOGTAG,
                "Unable to delete cached tile from " + pTileSourceInfo.name() + " " + MapTileIndex.toString(pMapTileIndex) + ", database not available."
            )
            Counters.fileCacheSaveErrors++
            return false
        }
        try {
            val index: Long = getIndex(pMapTileIndex)
            db.delete(DatabaseFileArchive.Companion.TABLE, primaryKey, getPrimaryKeyParameters(index, pTileSourceInfo))
            return true
        } catch (ex: Exception) {
            //note, although we check for db null state at the beginning of this method, it's possible for the
            //db to be closed during the execution of this method
            Log.e(
                IMapView.LOGTAG,
                "Unable to delete cached tile from " + pTileSourceInfo.name() + " " + MapTileIndex.toString(pMapTileIndex) + " db is " + (if (db == null) "null" else "not null"),
                ex
            )
            Counters.fileCacheSaveErrors++
            catchException(ex)
        }
        return false
    }

    /**
     * Returns the number of tiles in the cache for the specified tile source name
     *
     * @param tileSourceName
     * @return
     * @since 5.6
     */
    fun getRowCount(tileSourceName: String?): Long {
        if (tileSourceName == null) {
            return getRowCount(null, null)
        }
        return getRowCount(DatabaseFileArchive.Companion.COLUMN_PROVIDER + "=?", arrayOf<String?>(tileSourceName))
    }


    /**
     * Count cache tiles: helper method
     *
     * @return the number of tiles, or -1 if a problem occurred
     * @since 6.0.2
     */
    protected fun getRowCount(pWhereClause: String?, pWhereClauseArgs: Array<String?>?): Long {
        var cursor: Cursor? = null
        try {
            val db = this.db
            if (db == null || !db.isOpen()) {
                return -1
            }
            cursor = db.rawQuery(
                ("select count(*) from " + DatabaseFileArchive.Companion.TABLE
                        + (if (pWhereClause == null) "" else " where " + pWhereClause)), pWhereClauseArgs
            )
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        } catch (ex: Exception) {
            catchException(ex)
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
        return -1
    }

    /**
     * Count cache tiles
     *
     * @param pTileSourceName the tile source name (possibly null)
     * @param pZoom           the zoom level
     * @param pInclude        a collection of bounding boxes to include (possibly null/empty)
     * @param pExclude        a collection of bounding boxes to exclude (possibly null/empty)
     * @return the number of corresponding tiles in the cache
     * @since 6.0.2
     */
    fun getRowCount(
        pTileSourceName: String?, pZoom: Int,
        pInclude: MutableCollection<Rect?>?, pExclude: MutableCollection<Rect?>?
    ): Long {
        return getRowCount(
            getWhereClause(pZoom, pInclude, pExclude)
                .toString() + (if (pTileSourceName != null) " and " + DatabaseFileArchive.Companion.COLUMN_PROVIDER + "=?" else ""),
            if (pTileSourceName != null) arrayOf<String?>(pTileSourceName) else null
        )
    }

    val size: Long
        /**
         * Returns the size of the database file in bytes.
         */
        get() = db_file!!.length()

    val firstExpiry: Long
        /**
         * Returns the expiry time of the tile that expires first.
         */
        get() {
            val db = this.db
            if (db == null || !db.isOpen()) {
                return 0
            }
            try {
                val cursor =
                    db.rawQuery("select min(" + COLUMN_EXPIRES + ") from " + DatabaseFileArchive.Companion.TABLE, null)
                cursor.moveToFirst()
                val time = cursor.getLong(0)
                cursor.close()
                return time
            } catch (ex: Exception) {
                Log.e(IMapView.LOGTAG, "Unable to query for oldest tile", ex)
                catchException(ex)
            }
            return 0
        }

    override fun getExpirationTimestamp(pTileSource: ITileSource?, pMapTileIndex: Long): Long? {
        if (pTileSource == null) return null
        var cursor: Cursor? = null
        try {
            cursor = getTileCursor(getPrimaryKeyParameters(getIndex(pMapTileIndex), pTileSource), expireQueryColumn)
            if (cursor!!.moveToNext()) {
                return cursor.getLong(0)
            }
        } catch (ex: Exception) {
            Log.e(IMapView.LOGTAG, "error getting expiration date from the tile cache", ex)
            catchException(ex)
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
        return null
    }

    /**
     * @param pPrimaryKeyParameters
     * @param pColumns
     * @return
     * @since 5.6.5
     */
    fun getTileCursor(pPrimaryKeyParameters: Array<String?>?, pColumns: Array<String?>?): Cursor? {
        val db = this.db
        return db.query(DatabaseFileArchive.Companion.TABLE, pColumns, primaryKey, pPrimaryKeyParameters, null, null, null)
    }

    init {
        this.db

        if (!hasInited) {
            hasInited = true

            if (cleanOnStartup) {
                garbageCollector.gc()
            }
        }
    }

    @Throws(Exception::class)
    override fun loadTile(pTileSource: ITileSource?, pMapTileIndex: Long): Drawable? {
        if (pTileSource == null) return null
        var bits: ByteArray? = null
        var expirationTimestamp: Long = 0
        var cur: Cursor? = null
        try {
            val index: Long = getIndex(pMapTileIndex)
            cur = getTileCursor(getPrimaryKeyParameters(index, pTileSource), queryColumns)
            if (cur!!.moveToFirst()) {
                bits = cur.getBlob(0)
                expirationTimestamp = cur.getLong(1)
            }
            if (bits == null) {
                if (instance!!.isDebugMode) {
                    Log.d(IMapView.LOGTAG, "SqlCache - Tile doesn't exist: " + pTileSource.name() + MapTileIndex.toString(pMapTileIndex))
                }
                return null
            }
        } catch (ex: Exception) {
            catchException(ex)
            throw ex
        } finally {
            if (cur != null) {
                cur.close()
            }
        }

        var inputStream: InputStream? = null
        try {
            inputStream = ByteArrayInputStream(bits)
            val drawable = pTileSource.getDrawable(inputStream)
            // Check to see if file has expired
            val now = System.currentTimeMillis()
            val fileExpired = expirationTimestamp < now

            if (fileExpired && drawable != null) {
                if (instance!!.isDebugMode) {
                    Log.d(IMapView.LOGTAG, "Tile expired: " + pTileSource.name() + MapTileIndex.toString(pMapTileIndex))
                }
                ExpirableBitmapDrawable.Companion.setState(drawable, ExpirableBitmapDrawable.Companion.EXPIRED)
            }
            return drawable
        } finally {
            if (inputStream != null) {
                StreamUtils.closeStream(inputStream)
            }
        }
    }

    /**
     * @param pToBeDeleted      Amount of bytes to delete (as tile blob size)
     * @param pBulkSize         Number of tiles to delete in bulk
     * @param pPauseMillis      Pause between bulk actions, in order not to play it not aggressive on the CPU
     * @param pIncludeUnexpired Should we also delete tiles that are not expired?
     * @since 6.0.2
     */
    fun runCleanupOperation(
        pToBeDeleted: Long, pBulkSize: Int,
        pPauseMillis: Long, pIncludeUnexpired: Boolean
    ) {
        var diff = pToBeDeleted
        val where = StringBuilder()
        var sep: String?
        var first = true
        val db = this.db
        while (diff > 0) {
            if (first) {
                first = false
            } else {
                if (pPauseMillis > 0) {
                    try {
                        Thread.sleep(pPauseMillis)
                    } catch (e: InterruptedException) {
                        //
                    }
                }
            }
            val now = System.currentTimeMillis()
            val cur: Cursor
            try {
                cur = db.rawQuery(
                    "SELECT " + DatabaseFileArchive.Companion.COLUMN_KEY + ",LENGTH(HEX(" + DatabaseFileArchive.Companion.COLUMN_TILE + "))/2 " +
                            "FROM " + DatabaseFileArchive.Companion.TABLE + " " +
                            "WHERE " +
                            COLUMN_EXPIRES + " IS NOT NULL " +
                            (if (pIncludeUnexpired) "" else "AND " + COLUMN_EXPIRES + " < " + now + " ") +
                            "ORDER BY " + COLUMN_EXPIRES + " ASC " +
                            "LIMIT " + pBulkSize, null
                )
            } catch (e: Exception) {
                catchException(e)
                return
            }
            cur.moveToFirst()
            where.setLength(0)
            where.append(DatabaseFileArchive.Companion.COLUMN_KEY + " in (")
            sep = ""
            while (!cur.isAfterLast()) {
                val key = cur.getLong(0)
                val size = cur.getLong(1)
                cur.moveToNext()

                where.append(sep).append(key)
                sep = ","
                diff -= size
                if (diff <= 0) { // we already have enough tiles to delete
                    break
                }
            }
            cur.close()
            if ("" == sep) { // nothing to delete
                return
            }
            where.append(')')
            try {
                db.delete(DatabaseFileArchive.Companion.TABLE, where.toString(), null)
            } catch (e: SQLiteFullException) {
                Log.e(IMapView.LOGTAG, "SQLiteFullException while cleanup.", e)
                catchException(e)
            } catch (e: Exception) {
                catchException(e)
                return
            }
        }
    }

    protected val db: SQLiteDatabase
        /**
         * @since 6.0.2
         */
        get() {
            if (mDb != null) {
                return mDb!!
            }
            synchronized(mLock) {
                instance!!.osmdroidTileCache!!.mkdirs()
                db_file =
                    File(instance!!.osmdroidTileCache!!.getAbsolutePath() + File.separator + DATABASE_FILENAME)
                if (mDb == null) {
                    try {
                        mDb =
                            SQLiteDatabase.openOrCreateDatabase(db_file!!, null)
                        mDb!!.execSQL("CREATE TABLE IF NOT EXISTS " + DatabaseFileArchive.Companion.TABLE + " (" + DatabaseFileArchive.Companion.COLUMN_KEY + " INTEGER , " + DatabaseFileArchive.Companion.COLUMN_PROVIDER + " TEXT, " + DatabaseFileArchive.Companion.COLUMN_TILE + " BLOB, " + COLUMN_EXPIRES + " INTEGER, PRIMARY KEY (" + DatabaseFileArchive.Companion.COLUMN_KEY + ", " + DatabaseFileArchive.Companion.COLUMN_PROVIDER + "));")
                    } catch (ex: Exception) {
                        Log.e(IMapView.LOGTAG, "Unable to start the sqlite tile writer. Check external storage availability.", ex)
                        catchException(ex)
                        throw IllegalStateException("Unable to open sqlite tile cache", ex)
                    }
                }
            }
            return mDb!!
        }

    /**
     * @since 6.0.2
     */
    fun refreshDb() {
        synchronized(mLock) {
            if (mDb != null) {
                mDb!!.close()
                mDb = null
            }
        }
    }

    /**
     * @since 6.0.2
     */
    protected fun catchException(pException: Exception?) {
        if (pException is SQLiteException) {
            if (!isFunctionalException(pException)) {
                refreshDb()
            }
        }
    }

    /**
     * @since 6.0.2
     */
    private fun createIndex(pDb: SQLiteDatabase) {
        pDb.execSQL("CREATE INDEX IF NOT EXISTS " + COLUMN_EXPIRES_INDEX + " ON " + DatabaseFileArchive.Companion.TABLE + " (" + COLUMN_EXPIRES + ");")
    }

    /**
     * @since 6.0.2
     */
    override fun runDuringSplashScreen() {
        val db = this.db
        createIndex(db)
    }

    /**
     * @return the part of a SQL where clause used to restrict the selected tiles to
     * - a zoom
     * - a bounding box (possibly null)
     * @since 6.0.2
     */
    protected fun getWhereClause(pZoom: Int, pRect: Rect?): StringBuilder {
        val maxValueForZoom = (-1 + (1 shl (pZoom + 1))).toLong()
        val firstIndexForZoom: Long = getIndex(0, 0, pZoom.toLong())
        val lastIndexForZoom: Long = getIndex(maxValueForZoom, maxValueForZoom, pZoom.toLong())
        val xForZoom: String = extractXFromKeyInSQL(pZoom)
        val yForZoom: String = extractYFromKeyInSQL(pZoom)

        val buffer = StringBuilder()
        buffer.append('(')
        buffer.append(DatabaseFileArchive.Companion.COLUMN_KEY).append(" between ")
            .append(firstIndexForZoom).append(" and ").append(lastIndexForZoom)
        if (pRect != null) {
            buffer.append(" and ")
            if (pRect.left == pRect.right) {
                buffer.append(xForZoom).append("=").append(pRect.left)
            } else {
                buffer.append("(")
                    .append(xForZoom).append(">=").append(pRect.left)
                    .append(if (pRect.left < pRect.right) " and " else " or ")
                    .append(xForZoom).append("<=").append(pRect.right)
                    .append(")")
            }
            buffer.append(" and ")
            if (pRect.top == pRect.bottom) {
                buffer.append(yForZoom).append("=").append(pRect.top)
            } else {
                buffer.append("(")
                    .append(yForZoom).append(">=").append(pRect.top)
                    .append(if (pRect.top < pRect.bottom) " and " else " or ")
                    .append(yForZoom).append("<=").append(pRect.bottom)
                    .append(")")
            }
        }
        buffer.append(')')
        return buffer
    }

    /**
     * @return the part of a SQL where clause used to restrict the selected tiles to
     * - a zoom
     * - a collection of bounding boxes to include (possibly null/empty)
     * - a collection of bounding boxes to exclude (possibly null/empty)
     * @since 6.0.2
     */
    protected fun getWhereClause(
        pZoom: Int,
        pInclude: MutableCollection<Rect?>?,
        pExclude: MutableCollection<Rect?>?
    ): StringBuilder {
        val buffer = StringBuilder()
        buffer.append('(')
        buffer.append(getWhereClause(pZoom, null))
        if (pInclude != null && pInclude.size > 0) {
            buffer.append(" and (")
            var coordinator = ""
            for (rect in pInclude) {
                buffer.append(coordinator).append('(').append(getWhereClause(pZoom, rect)).append(')')
                coordinator = " or "
            }
            buffer.append(")")
        }
        if (pExclude != null && pExclude.size > 0) {
            buffer.append(" and not(")
            var coordinator = ""
            for (rect in pExclude) {
                buffer.append(coordinator).append('(').append(getWhereClause(pZoom, rect)).append(')')
                coordinator = " or "
            }
            buffer.append(")")
        }
        buffer.append(')')
        return buffer
    }

    /**
     * Delete cache tiles
     *
     * @param pTileSourceName the tile source name (possibly null)
     * @param pZoom           the zoom level
     * @param pInclude        a collection of bounding boxes to include (possibly null/empty)
     * @param pExclude        a collection of bounding boxes to exclude (possibly null/empty)
     * @return the number of corresponding tiles deleted from the cache, or -1 if a problem occurred
     * @since 6.0.2
     */
    fun delete(
        pTileSourceName: String?, pZoom: Int,
        pInclude: MutableCollection<Rect?>?, pExclude: MutableCollection<Rect?>?
    ): Long {
        try {
            val db = this.db
            if (db == null || !db.isOpen()) {
                return -1
            }
            return db.delete(
                DatabaseFileArchive.Companion.TABLE,
                getWhereClause(pZoom, pInclude, pExclude)
                    .toString() + (if (pTileSourceName != null) " and " + DatabaseFileArchive.Companion.COLUMN_PROVIDER + "=?" else ""),
                if (pTileSourceName != null) arrayOf<String?>(pTileSourceName) else null
            ).toLong()
        } catch (ex: Exception) {
            catchException(ex)
            return 0
        }
    }

    companion object {
        const val DATABASE_FILENAME: String = "cache.db"
        const val COLUMN_EXPIRES: String = "expires"
        const val COLUMN_EXPIRES_INDEX: String = "expires_index"

        private var cleanOnStartup = true

        /*
      * disables cache purge of expired tiled on start up
     * if this is set to false, the database will only purge tiles if manually called or if
     * the storage device runs out of space.
     *
     * expired tiles will continue to be overwritten as new versions are downloaded regardless
    @since 6.0.0
     */
        fun setCleanupOnStart(value: Boolean) {
            cleanOnStartup = value
        }

        private val mLock = Any()
        protected var db_file: File? = null
        protected var mDb: SQLiteDatabase? = null
        var hasInited: Boolean = false

        /**
         * @return a composite key designed as a SQL primary key that includes X, Y and zoom
         * @see .extractXFromKeyInSQL
         * @see .extractYFromKeyInSQL
         * @since 5.6.5
         */
        fun getIndex(pX: Long, pY: Long, pZ: Long): Long {
            return ((pZ shl pZ.toInt()) + pX shl pZ.toInt()) + pY
        }

        /**
         * @return the SQL formula to extract X from the table key for a given zoom level
         * @see .getIndex
         * @since 6.0.2
         */
        protected fun extractXFromKeyInSQL(pZoom: Int): String {
            return "((" + DatabaseFileArchive.Companion.COLUMN_KEY + ">>" + pZoom + ")%" + (1 shl pZoom) + ")"
        }

        /**
         * @return the SQL formula to extract Y from the table key for a given zoom level
         * @see .getIndex
         * @since 6.0.2
         */
        protected fun extractYFromKeyInSQL(pZoom: Int): String {
            return "(" + DatabaseFileArchive.Companion.COLUMN_KEY + "%" + (1 shl pZoom) + ")"
        }

        /**
         * Gets the single column index value for a map tile
         * Unluckily, "map tile index" and "sql pk" don't match
         *
         * @param pMapTileIndex
         * @since 5.6.5
         */
        fun getIndex(pMapTileIndex: Long): Long {
            return getIndex(
                MapTileIndex.getX(pMapTileIndex).toLong(),
                MapTileIndex.getY(pMapTileIndex).toLong(),
                MapTileIndex.getZoom(pMapTileIndex).toLong()
            )
        }

        /**
         * @since 5.6.5
         */
        val primaryKey: String = DatabaseFileArchive.Companion.COLUMN_KEY + "=? and " + DatabaseFileArchive.Companion.COLUMN_PROVIDER + "=?"

        /**
         * @param pIndex
         * @param pTileSourceInfo
         * @return
         * @since 5.6.5
         */
        fun getPrimaryKeyParameters(pIndex: Long, pTileSourceInfo: ITileSource): Array<String?> {
            return getPrimaryKeyParameters(pIndex, pTileSourceInfo.name())
        }

        /**
         * @param pIndex
         * @param pTileSourceInfo
         * @return
         * @since 5.6.5
         */
        fun getPrimaryKeyParameters(pIndex: Long, pTileSourceInfo: String?): Array<String?> {
            return arrayOf<String?>(pIndex.toString(), pTileSourceInfo)
        }

        /**
         * For optimization reasons
         *
         * @since 5.6.5
         */
        private val queryColumns = arrayOf<String?>(DatabaseFileArchive.Companion.COLUMN_TILE, COLUMN_EXPIRES)

        /**
         * For optimization reasons
         *
         * @since 5.6.5
         */
        private val expireQueryColumn = arrayOf<String?>(COLUMN_EXPIRES)

        /**
         * @return true if it's a mere functional exception (poor SQL code for instance)
         * and false if it's something potentially more serious (no more SQLite database for instance)
         * @since 6.0.2
         */
        fun isFunctionalException(pSQLiteException: SQLiteException): Boolean {
            when (pSQLiteException.javaClass.getSimpleName()) {
                "SQLiteBindOrColumnIndexOutOfRangeException", "SQLiteBlobTooBigException", "SQLiteConstraintException", "SQLiteDatatypeMismatchException", "SQLiteFullException", "SQLiteMisuseException", "SQLiteTableLockedException" -> return true
                "SQLiteAbortException", "SQLiteAccessPermException", "SQLiteCantOpenDatabaseException", "SQLiteDatabaseCorruptException", "SQLiteDatabaseLockedException", "SQLiteDiskIOException", "SQLiteDoneException", "SQLiteOutOfMemoryException", "SQLiteReadOnlyDatabaseException" -> return false
                else -> return false
            }
        }
    }
}

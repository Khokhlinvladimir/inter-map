package org.osmdroid.debug.model

import android.database.Cursor
import org.osmdroid.tileprovider.modules.DatabaseFileArchive
import org.osmdroid.tileprovider.modules.SqlTileWriter

/**
 * Extended the sqlite tile writer to have some additional query functions. A this point
 * it's unclear if there is a need to put these with the osmdroid-android library, thus they were
 * put here as more of an example.
 *
 *
 * created on 12/21/2016.
 *
 * @author Alex O'Ree
 * @since 5.6.2
 */
class SqlTileWriterExt : SqlTileWriter() {
    fun select(rows: Int, offset: Int): Cursor? {
        val db = getDb()
        if (db != null) return db.rawQuery(
            "select " + DatabaseFileArchive.COLUMN_KEY + "," + COLUMN_EXPIRES + "," + DatabaseFileArchive.COLUMN_PROVIDER + " from " + DatabaseFileArchive.TABLE + " limit ? offset ?",
            arrayOf<String>(rows.toString() + "", offset.toString() + "")
        )
        return null
    }

    val sources: MutableList<SourceCount?>
        /**
         * gets all the tiles sources that we have tiles for in the cache database and their counts
         *
         * @return
         */
        get() {
            val db = getDb()
            val ret: MutableList<SourceCount?> = ArrayList<SourceCount?>()
            if (db == null) {
                return ret
            }
            var cur: Cursor? = null
            try {
                cur = db.rawQuery(
                    ("select "
                            + DatabaseFileArchive.COLUMN_PROVIDER
                            + ",count(*) "
                            + ",min(length(" + DatabaseFileArchive.COLUMN_TILE + ")) "
                            + ",max(length(" + DatabaseFileArchive.COLUMN_TILE + ")) "
                            + ",sum(length(" + DatabaseFileArchive.COLUMN_TILE + ")) "
                            + "from " + DatabaseFileArchive.TABLE + " "
                            + "group by " + DatabaseFileArchive.COLUMN_PROVIDER), null
                )
                while (cur.moveToNext()) {
                    val c = SourceCount()
                    c.source = cur.getString(0)
                    c.rowCount = cur.getLong(1)
                    c.sizeMin = cur.getLong(2)
                    c.sizeMax = cur.getLong(3)
                    c.sizeTotal = cur.getLong(4)
                    c.sizeAvg = c.sizeTotal / c.rowCount
                    ret.add(c)
                }
            } catch (e: Exception) {
                catchException(e)
            } finally {
                if (cur != null) {
                    cur.close()
                }
            }
            return ret
        }

    val rowCountExpired: Long
        get() = getRowCount(
            COLUMN_EXPIRES + "<?",
            arrayOf<String>(System.currentTimeMillis().toString())
        )

    class SourceCount {
        var rowCount: Long = 0
        var source: String? = null
        var sizeTotal: Long = 0
        var sizeMin: Long = 0
        var sizeMax: Long = 0
        var sizeAvg: Long = 0
    }
}

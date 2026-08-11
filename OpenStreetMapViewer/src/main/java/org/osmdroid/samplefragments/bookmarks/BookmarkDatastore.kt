package org.osmdroid.samplefragments.bookmarks

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.UUID

/**
 * created on 2/11/2018.
 *
 * @author Alex O'Ree
 */
class BookmarkDatastore {
    protected var db_file: File?

    protected var mDatabase: SQLiteDatabase? = null

    init {
        instance!!.osmdroidTileCache!!.mkdirs()
        db_file = File(instance!!.osmdroidTileCache!!.getAbsolutePath() + File.separator + DATABASE_FILENAME)


        try {
            mDatabase = SQLiteDatabase.openOrCreateDatabase(db_file!!, null)
            mDatabase!!.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                        COLUMN_LAT + " INTEGER , " +
                        COLUMN_LON + " INTEGER, " +
                        COLUMN_TITLE + " TEXT, " +
                        COLUMN_ID + " TEXT, " +
                        COLUMN_DESC + " TEXT, PRIMARY KEY (" + COLUMN_ID + ") );"
            )
        } catch (ex: Throwable) {
            Log.e(IMapView.LOGTAG, "Unable to start the bookmark database. Check external storage availability.", ex)
        }
    }

    //TODO geopgrahpic bounding box?
    fun getBookmarksAsMarkers(view: MapView?): MutableList<Marker?> {
        val markers: MutableList<Marker?> = ArrayList<Marker?>()
        try {
            //TODO order by title
            val cur = mDatabase!!.rawQuery("SELECT * FROM " + TABLE, null)
            while (cur.moveToNext()) {
                val m = Marker(view!!)
                m.setId(cur.getString(cur.getColumnIndex(COLUMN_ID)))
                m.setTitle(cur.getString(cur.getColumnIndex(COLUMN_TITLE)))
                m.setSubDescription(cur.getString(cur.getColumnIndex(COLUMN_DESC)))
                m.position = GeoPoint(cur.getDouble(cur.getColumnIndex(COLUMN_LAT)), cur.getDouble(cur.getColumnIndex(COLUMN_LON)))
                m.setSnippet(m.position.toDoubleString())

                markers.add(m)
            }
            cur.close()
        } catch (e: Exception) {
            Log.w(IMapView.LOGTAG, "Error getting tile sources: ", e)
        }
        return markers
    }


    fun addBookmark(bookmark: Marker) {
        addBookmark(
            bookmark.getId(),
            bookmark.position.latitude,
            bookmark.position.longitude,
            bookmark.getTitle(),
            bookmark.getSubDescription()
        )
    }


    fun removeBookmark(bookmark: Marker) {
        removeBookmark(bookmark.getId())
    }


    fun removeBookmark(id: String?) {
        mDatabase!!.delete(TABLE, COLUMN_ID, arrayOf<String>(COLUMN_ID))
    }


    fun addBookmark(id: String?, lat: Double, lon: Double, title: String?, description: String?) {
        val cv = ContentValues()
        if (id == null || id.length == 0) cv.put(COLUMN_ID, UUID.randomUUID().toString())
        else {
            mDatabase!!.delete(TABLE, COLUMN_ID + "=?", arrayOf<String>(id))
            cv.put(COLUMN_ID, id)
        }

        cv.put(COLUMN_LAT, lat)
        cv.put(COLUMN_LON, lon)
        cv.put(COLUMN_DESC, description)
        cv.put(COLUMN_TITLE, title)
        mDatabase!!.insert(TABLE, null, cv)
    }

    fun close() {
        db_file = null
        mDatabase!!.close()
        mDatabase = null
    }

    companion object {
        const val TABLE: String = "bookmarks"
        const val COLUMN_ID: String = "markerid"
        const val COLUMN_LAT: String = "lat"
        const val COLUMN_LON: String = "lon"
        const val COLUMN_TITLE: String = "title"
        const val COLUMN_DESC: String = "description"
        const val DATABASE_FILENAME: String = "bookmarks.mDatabase"
    }
}

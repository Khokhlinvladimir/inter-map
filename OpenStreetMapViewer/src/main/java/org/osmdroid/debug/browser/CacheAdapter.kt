package org.osmdroid.debug.browser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.debug.model.MapTileExt
import org.osmdroid.debug.model.SqlTileWriterExt
import org.osmdroid.debug.util.FileDateUtil
import org.osmdroid.debug.util.HumanTime
import org.osmdroid.tileprovider.modules.DatabaseFileArchive
import org.osmdroid.tileprovider.modules.SqlTileWriter

/**
 * basic listview adapter
 * created on 12/20/2016.
 *
 * @author Alex O'Ree
 * @since 5.6.2
 */
class CacheAdapter(context: Context, var cursor: SqlTileWriterExt) : ArrayAdapter<Any?>(context, R.layout.item_cache) {
    override fun getCount(): Int {
        return cursor.getRowCount(null).toInt()
    }

    override fun getItem(id: Int): Any? {
        val select = cursor.select(1, id) ?: return null
        if (select.moveToNext()) {
            val tile = MapTileExt()
            tile.key = select.getLong(select.getColumnIndex(DatabaseFileArchive.COLUMN_KEY))
            tile.source = select.getString(select.getColumnIndex(DatabaseFileArchive.COLUMN_PROVIDER))
            if (!select.isNull(select.getColumnIndex(SqlTileWriter.COLUMN_EXPIRES))) {
                tile.expires = select.getLong(select.getColumnIndex(SqlTileWriter.COLUMN_EXPIRES))
            } else {
                tile.expires = null
            }
            select.close()
            return tile
        }
        select.close()
        return null
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView
            ?: LayoutInflater.from(getContext()).inflate(R.layout.item_cache, parent, false)
        val p = getItem(position) as MapTileExt?
        if (p != null) {
            // Find fields to populate in inflated template
            val source = itemView.findViewById<TextView>(R.id.tvSource)
            val key = itemView.findViewById<TextView>(R.id.tvDbKey)
            val expires = itemView.findViewById<TextView>(R.id.tvExpires)

            source.setText(p.source)
            key.setText(p.key.toString() + "")
            if (p.expires == null) {
                expires.setText("null!")
            } else {
                val time = p.expires!!
                //time should be in the future
                var durationUtilExpires = FileDateUtil.getModifiedDate(time)
                if (time > System.currentTimeMillis()) {
                    //has not expired yet
                    durationUtilExpires += "\nValid for " + HumanTime.approximately(time - System.currentTimeMillis())
                } else {
                    //expired already
                    durationUtilExpires += "\nExpired at " + HumanTime.approximately(System.currentTimeMillis() - time)
                }
                expires.setText(durationUtilExpires)
            }
        }


        return itemView
    }
}

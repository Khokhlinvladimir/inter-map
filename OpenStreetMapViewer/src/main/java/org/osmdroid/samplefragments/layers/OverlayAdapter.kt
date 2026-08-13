package org.osmdroid.samplefragments.layers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayManager
import org.osmdroid.views.overlay.OverlayWithIW

/**
 * created on 2/18/2018.
 *
 * @author Alex O'Ree
 */
class OverlayAdapter(context: Context, var manager: OverlayManager?) : ArrayAdapter<Any?>(context, R.layout.drawer_list_item) {
    private val adapterContext: Context = context

    override fun getCount(): Int {
        synchronized(manager!!) {
            if (manager != null) return manager!!.size
            return 0
        }
    }

    override fun getItem(position: Int): Overlay? {
        return manager!!.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = adapterContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.drawer_list_item, parent, false)
        val view = rowView.findViewById<TextView>(R.id.itemText)

        val overlay = getItem(position)
        if (overlay != null) {
            if (overlay is OverlayWithIW) {
                var title = overlay.getTitle()
                if (title == null || title.length == 0) title = overlay.javaClass.getSimpleName()
                view.setText(title)
            } else view.setText(overlay.javaClass.getSimpleName())
        }
        return rowView
    }
}

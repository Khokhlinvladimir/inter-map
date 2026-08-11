package org.osmdroid.intro

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.tileprovider.util.StorageUtils
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * created on 1/18/2017.
 *
 * @author Alex O'Ree
 */
class StorageAdapter(context: Context, var data: MutableList<StorageUtils.StorageInfo?>) :
    ArrayAdapter<Any?>(context, R.layout.layout_storage_device) {
    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(id: Int): Any? {
        return data.get(id)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView
            ?: LayoutInflater.from(getContext()).inflate(R.layout.layout_storage_device, parent, false)
        val info = getItem(position) as StorageUtils.StorageInfo?

        if (info != null) {
            // Find fields to populate in inflated template
            val drive = itemView.findViewById<TextView>(R.id.storageName)
            val frespace = itemView.findViewById<TextView>(R.id.storageFreespace)
            val path = itemView.findViewById<TextView>(R.id.storagePath)
            drive.setText(info.displayName)
            frespace.setText("Free space: " + readableFileSize(info.freeSpace))
            path.setText(info.path)
        }


        return itemView
    }

    companion object {
        @JvmStatic
        fun readableFileSize(size: Long): String {
            if (size <= 0) return "0"
            val units: Array<String> = arrayOf("B", "kB", "MB", "GB", "TB")
            val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
            return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
        }
    }
}

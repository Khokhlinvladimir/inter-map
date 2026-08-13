package org.osmdroid.samplefragments.layouts.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.R
import org.osmdroid.samplefragments.layouts.list.dummy.DummyContent.DummyItem

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * 99% is this is boiler plate android studio generated stuff
 */
class MyStreetAddressRecyclerViewAdapter(private val mValues: MutableList<DummyItem?>) :
    RecyclerView.Adapter<MyStreetAddressRecyclerViewAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.fragment_streetaddress, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues.get(position)
        holder.mIdView.setText(mValues.get(position)!!.id)
        holder.mContentView.setText(mValues.get(position)!!.content)

        holder.mView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                //TODO
            }
        })
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView
        val mContentView: TextView
        var mItem: DummyItem? = null

        init {
            mIdView = mView.findViewById<TextView>(R.id.id)
            mContentView = mView.findViewById<TextView>(R.id.content)
        }

        override fun toString(): String {
            return super.toString() + " '" + mContentView.getText() + "'"
        }
    }
}

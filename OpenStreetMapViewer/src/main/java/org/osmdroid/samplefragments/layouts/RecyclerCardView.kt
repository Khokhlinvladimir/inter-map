package org.osmdroid.samplefragments.layouts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.layouts.rec.ConstructorInfoData
import org.osmdroid.samplefragments.layouts.rec.CustomRecycler
import org.osmdroid.samplefragments.layouts.rec.Info
import org.osmdroid.views.MapView

/**
 * created on 1/13/2017.
 *
 * @author Alex O'Ree
 */
class RecyclerCardView : BaseSampleFragment() {
    //Objects for RecyclerView and InfoData
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: CustomRecycler? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    override val sampleTitle: String
        get() = "Map in a recycler/cardview layout"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.recyclerview, null)
        mMapView = v.findViewById<MapView?>(R.id.mapview)


        //Load Data And RecyclverView
        val a: ArrayList<Info>
        val b = ConstructorInfoData()
        a = b.obtainData()
        mRecyclerView = v.findViewById<RecyclerView>(R.id.recyclerView)
        mLayoutManager = LinearLayoutManager(getContext())

        mRecyclerView!!.setLayoutManager(mLayoutManager)
        //Adapter is created in the last step
        mAdapter = CustomRecycler(a)
        mRecyclerView!!.setAdapter(mAdapter)

        return v
    }
}

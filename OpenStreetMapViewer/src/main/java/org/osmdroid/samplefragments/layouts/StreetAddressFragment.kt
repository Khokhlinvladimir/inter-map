package org.osmdroid.samplefragments.layouts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.layouts.list.MyStreetAddressRecyclerViewAdapter
import org.osmdroid.samplefragments.layouts.list.dummy.DummyContent

/**
 * A fragment representing a list of Items.
 * 99% is this is boiler plate android studio generated stuff
 */
class StreetAddressFragment
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
    : BaseSampleFragment() {
    // TODO: Customize parameters
    private val mColumnCount = 1

    override val sampleTitle: String
        get() = "Map in a List View"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_streetaddress_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            val context = view.getContext()
            val recyclerView = view
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(LinearLayoutManager(context))
            } else {
                recyclerView.setLayoutManager(GridLayoutManager(context, mColumnCount))
            }
            recyclerView.setAdapter(MyStreetAddressRecyclerViewAdapter(DummyContent.ITEMS))
        }
        return view
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }
}

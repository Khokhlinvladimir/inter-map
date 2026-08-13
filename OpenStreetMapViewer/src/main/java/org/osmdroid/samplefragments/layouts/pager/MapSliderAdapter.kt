package org.osmdroid.samplefragments.layouts.pager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * Created by alex on 10/22/16.
 */
class MapSliderAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        when (position) {
            0 -> return SimpleTextFragment()
            1 -> return MapFragment()
            2 -> return WebviewFragment()
        }
        throw IndexOutOfBoundsException("Unknown page: $position")
    }

    override fun getCount(): Int {
        return 3
    }
}

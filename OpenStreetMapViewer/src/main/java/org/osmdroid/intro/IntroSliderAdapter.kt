package org.osmdroid.intro

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * Created by alex on 10/22/16.
 */
class IntroSliderAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        when (position) {
            0 -> return LogoFragment()
            1 -> return AboutFragment()
            2 -> return PermissionsFragment()
            3 -> return StoragePreferenceFragment()
            4 -> return DataUseWarning()
            5 -> return TileSourceWarnings()
        }
        throw IndexOutOfBoundsException("Unknown intro page: $position")
    }

    override fun getCount(): Int {
        return 6
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        println("New pager is " + position)
    }
}

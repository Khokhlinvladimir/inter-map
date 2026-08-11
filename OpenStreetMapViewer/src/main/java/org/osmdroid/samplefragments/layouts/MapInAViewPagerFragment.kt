package org.osmdroid.samplefragments.layouts

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.layouts.pager.MapSliderAdapter

/**
 * Created by alex on 10/22/16.
 */
class MapInAViewPagerFragment : BaseSampleFragment() {
    var mPager: ViewPager? = null
    var mPagerAdapter: PagerAdapter? = null

    override val sampleTitle: String
        get() = "Map in a view pager"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.map_viewpager, null)
        mPager = v.findViewById<ViewPager>(R.id.pager)
        mPagerAdapter = MapSliderAdapter(getActivity()!!.getSupportFragmentManager())
        mPager!!.setAdapter(mPagerAdapter)
        return v
    }

    public override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated")
    }

    public override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDetach")
    }

    public override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    public override fun skipOnCiTests(): Boolean {
        return true
    }

    public override fun runTestProcedures() {
        val act: Activity? = getActivity()
        var count = 0
        while (act == null && count < 10) {
            count++
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
            }
        }
        if (act == null) throw RuntimeException("fragment was never attached to an activity")
        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                mPager!!.setCurrentItem(0, true)
            }
        })
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                mPager!!.setCurrentItem(1, true)
            }
        })
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                mPager!!.setCurrentItem(2, true)
            }
        })
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                mPager!!.setCurrentItem(1, true)
            }
        })

        getActivity()!!.runOnUiThread(object : Runnable {
            override fun run() {
                mPager!!.setCurrentItem(0, true)
            }
        })
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}

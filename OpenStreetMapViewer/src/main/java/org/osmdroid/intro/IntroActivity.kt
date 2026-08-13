package org.osmdroid.intro

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import org.osmdroid.MainActivity
import org.osmdroid.R

/**
 * Intro activity, this is a simple intro to osmdroid, some legal stuff, tile storage preference, etc
 *
 *
 * created on 1/5/2017.
 *
 * @author Alex O'Ree
 */
class IntroActivity : FragmentActivity(), View.OnClickListener, OnPageChangeListener {
    var introviewpager: ViewPager? = null
    var introProgressBar: ProgressBar? = null
    var adapter: IntroSliderAdapter? = null
    var next: Button? = null
    var prev: Button? = null
    var viewpagerCurrentPosition: Int = 0

    public override fun onCreate(savedInstanced: Bundle?) {
        super.onCreate(savedInstanced)

        //skip this nonsense
        if (PreferenceManager.getDefaultSharedPreferences(this).contains("osmdroid_first_ran")) {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
            finish()
        }


        setContentView(R.layout.intro_frame)
        introviewpager = findViewById<ViewPager>(R.id.introviewpager)
        adapter = IntroSliderAdapter(getSupportFragmentManager())
        introviewpager!!.setAdapter(adapter)
        introviewpager!!.addOnPageChangeListener(this)
        introProgressBar = findViewById<ProgressBar>(R.id.introProgressBar)
        introProgressBar!!.setMax(adapter!!.getCount() - 1)
        introProgressBar!!.setProgress(0)

        next = findViewById<Button>(R.id.introNext)
        prev = findViewById<Button>(R.id.introPrev)
        next!!.setOnClickListener(this)
        prev!!.setOnClickListener(this)
    }


    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.introNext -> if (viewpagerCurrentPosition + 1 < adapter!!.getCount()) introviewpager!!.setCurrentItem(
                viewpagerCurrentPosition + 1,
                true
            )
            else {
                val edit = PreferenceManager.getDefaultSharedPreferences(this).edit()
                edit.putString("osmdroid_first_ran", "yes")
                edit.commit()
                //next to MainActivity
                val i = Intent(this, MainActivity::class.java)
                startActivity(i)
                finish() //prevent the back button from returning to this activity
            }

            R.id.introPrev -> if (viewpagerCurrentPosition - 1 >= 0) introviewpager!!.setCurrentItem(viewpagerCurrentPosition - 1, true)


        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        viewpagerCurrentPosition = position
        introProgressBar!!.setProgress(position)
        if (position == 0) {
            prev!!.setVisibility(View.INVISIBLE)
        } else {
            prev!!.setVisibility(View.VISIBLE)
        }

        if (position == adapter!!.getCount() - 1) {
            next!!.setText(R.string.done)
        } else {
            next!!.setText(R.string.next)
        }
        if (position == 3) {
            //storage preference fragment, force the update since now permissions may have been granted
            val item = adapter!!.getItem(position) as StoragePreferenceFragment
            item.updateStorage(this)
        }
    }

    override fun onPageSelected(position: Int) {
    }

    override fun onPageScrollStateChanged(state: Int) {
    }
}

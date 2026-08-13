package org.osmdroid

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.bugtestfragments.BugFactory
import org.osmdroid.bugtestfragments.WeathForceActivity
import org.osmdroid.model.IBaseActivity
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.ui.SamplesMenuFragment

/**
 * Created by alex on 6/29/16.
 */
class BugsTestingActivity : AppCompatActivity() {
    var fragmentSamples: SamplesMenuFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivity.updateStoragePreferences(this) //needed for unit tests
        setContentView(R.layout.activity_extra_samples)

        val myToolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(myToolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        val fm = this.getSupportFragmentManager()
        if (fm.findFragmentByTag(SAMPLES_FRAGMENT_TAG) == null) {
            val extras: MutableList<IBaseActivity?> = ArrayList<IBaseActivity?>()
            extras.add(WeathForceActivity())
            extras.add(Bug1783Activity())
            fragmentSamples = SamplesMenuFragment.newInstance(BugFactory.instance, extras)
            fm.beginTransaction().add(R.id.samples_container, fragmentSamples!!, SAMPLES_FRAGMENT_TAG).commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    public override fun onDestroy() {
        super.onDestroy()
        fragmentSamples = null
    }

    /**
     * small example of keyboard events on the mapview
     * page up = zoom out
     * page down = zoom in
     *
     * @param keyCode
     * @param event
     * @return
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val frag = getSupportFragmentManager().findFragmentByTag(SAMPLES_FRAGMENT_TAG)
        if (frag == null) {
            return super.onKeyUp(keyCode, event)
        }
        if (frag !is BaseSampleFragment) {
            return super.onKeyUp(keyCode, event)
        }
        val mMapView = frag.getmMapView()
        if (mMapView == null) return super.onKeyUp(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                mMapView.controller!!.zoomIn()
                return true
            }

            KeyEvent.KEYCODE_PAGE_UP -> {
                mMapView.controller!!.zoomOut()
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        const val SAMPLES_FRAGMENT_TAG: String = "org.osmdroid.BUGS_FRAGMENT_TAG"
    }
}

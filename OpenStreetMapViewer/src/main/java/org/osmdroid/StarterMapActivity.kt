// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.MainActivity.Companion.updateStoragePreferences

/**
 * Default map view activity.
 *
 * @author Manuel Stahl
 */
class StarterMapActivity : AppCompatActivity() {
    /**
     * The idea behind that is to force a MapView refresh when switching from offline to online.
     * If you don't do that, the map may display - when online - approximated tiles
     * * that were computed when offline
     * * that could be replaced by downloaded tiles
     * * but as the display is not refreshed there's no try to get better tiles
     *
     * @since 6.0
     */
    private val networkReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                starterMapFragment!!.invalidateMapView()
            } catch (e: NullPointerException) {
                // lazy handling of an improbable NPE
            }
        }
    }

    private var starterMapFragment: StarterMapFragment? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_starter_main)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        updateStoragePreferences(this) //needed for unit tests

        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        val fm = this.getSupportFragmentManager()
        if (fm.findFragmentByTag(MAP_FRAGMENT_TAG) == null) {
            starterMapFragment = StarterMapFragment.newInstance()
            fm.beginTransaction().add(R.id.map_container, starterMapFragment!!, MAP_FRAGMENT_TAG).commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
        when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                starterMapFragment!!.zoomIn()
                return true
            }

            KeyEvent.KEYCODE_PAGE_UP -> {
                starterMapFragment!!.zoomOut()
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * @since 6.0
     */
    override fun onDestroy() {
        unregisterReceiver(networkReceiver)
        super.onDestroy()
    }

    companion object {
        private const val MAP_FRAGMENT_TAG = "org.osmdroid.MAP_FRAGMENT_TAG"
    }
}

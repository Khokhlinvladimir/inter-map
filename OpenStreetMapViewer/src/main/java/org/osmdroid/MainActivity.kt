// Created by plusminus on 18:23:13 - 03.10.2008
package org.osmdroid

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.text.format.Formatter
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.config.Configuration.instance
import org.osmdroid.debug.CacheAnalyzerActivity
import org.osmdroid.diag.DiagnosticsActivity
import org.osmdroid.intro.IntroActivity
import org.osmdroid.samples.SampleWithMinimapItemizedoverlay
import org.osmdroid.samples.SampleWithTilesOverlay
import org.osmdroid.samples.SampleWithTilesOverlayAndCustomTileSource
import org.osmdroid.tileprovider.modules.SqlTileWriter
import java.io.File

class MainActivity : AppCompatActivity(), OnItemClickListener {
    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val myToolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(myToolbar)
        // Generate a ListView with Sample Maps
        val list = ArrayList<String?>()
        list.add("OSMDroid Sample Map (Start Here)")
        list.add("Sample with ItemizedOverlay")
        list.add("Sample with TilesOverlay")
        list.add("Sample with TilesOverlay and custom TileSource")
        list.add("More Samples")

        list.add("Report a Bug")
        list.add("Settings")
        list.add("Bug Drivers")
        list.add("Diagnostics")
        list.add("View the Intro again")
        list.add("Licenses")
        list.add("Cache Analyzer")

        val lv = findViewById<ListView>(R.id.activitylist)
        val adapter: ArrayAdapter<*> = ArrayAdapter<String?>(this, android.R.layout.simple_list_item_1, list)

        lv.setAdapter(adapter)
        lv.setOnItemClickListener(this)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (position) {
            0 -> this.startActivity(Intent(this, StarterMapActivity::class.java))
            1 -> this.startActivity(Intent(this, SampleWithMinimapItemizedoverlay::class.java))
            2 -> this.startActivity(Intent(this, SampleWithTilesOverlay::class.java))
            3 -> this.startActivity(Intent(this, SampleWithTilesOverlayAndCustomTileSource::class.java))
            4 -> this.startActivity(Intent(this, ExtraSamplesActivity::class.java))
            5 -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/osmdroid/osmdroid/issues/new"))
                startActivity(browserIntent)
            }

            6 -> {
                val i = Intent(this, PreferenceActivity::class.java)
                startActivity(i)
            }

            7 -> this.startActivity(Intent(this, BugsTestingActivity::class.java))
            8 -> this.startActivity(Intent(this, DiagnosticsActivity::class.java))
            9 -> {
                //skip this nonsense
                val edit = PreferenceManager.getDefaultSharedPreferences(this).edit()
                edit.remove("osmdroid_first_ran")
                edit.commit()

                val intent = Intent(this, IntroActivity::class.java)
                startActivity(intent)
                finish()
            }

            10 -> {
                val i = Intent(this, LicenseActivity::class.java)
                startActivity(i)
            }

            11 -> {
                val starter = Intent(this, CacheAnalyzerActivity::class.java)
                startActivity(starter)
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        updateStorageInfo()
        checkForCrashLogs()
    }

    private fun checkForCrashLogs() {
        //look for osmdroid crash logs
        val root = Environment.getExternalStorageDirectory()
        val pathToMyAttachedFile = "/osmdroid/crash.log"
        val file = File(root, pathToMyAttachedFile)
        if (!file.exists() || !file.canRead()) {
            return
        }

        //if found, prompt user to send to
        //osmdroidbugs@gmail.com
        val dialogClickListener: DialogInterface.OnClickListener = object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        //Yes button clicked
                        val emailIntent = Intent(Intent.ACTION_SEND)
                        emailIntent.setType("text/plain")
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>("osmdroidbugs@gmail.com"))
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Open Map crash log")
                        emailIntent.putExtra(Intent.EXTRA_TEXT, "Log data")

                        val uri = Uri.fromFile(file)
                        emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
                        startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"))
                    }

                    DialogInterface.BUTTON_NEGATIVE ->                         //No button clicked
                        file.delete()
                }
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Crash logs")
        builder.setMessage(
            "Sorry, it looks like we crashed at some point, would you mind sending us the" +
                    "crash log?"
        ).setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()
    }

    /**
     * gets storage state and current cache size
     */
    private fun updateStorageInfo() {
        val cacheSize: Long = updateStoragePreferences(this)

        //cache management ends here
        var tv = findViewById<TextView>(org.osmdroid.R.id.sdcardstate_value)
        val state = Environment.getExternalStorageState()

        val mSdCardAvailable = Environment.MEDIA_MOUNTED == state
        tv.setText((if (mSdCardAvailable) "Mounted" else "Not Available"))
        if (!mSdCardAvailable) {
            tv.setTextColor(Color.RED)
            tv.setTypeface(null, Typeface.BOLD)
        }

        tv = findViewById<TextView>(org.osmdroid.R.id.version_text)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        tv.setText(versionName + " " + BuildConfig.BUILD_TYPE)

        tv = findViewById<TextView>(org.osmdroid.R.id.mainstorageInfo)
        tv.setText(
            instance!!.osmdroidTileCache!!.getAbsolutePath() + "\n" +
                    "Cache size: " + Formatter.formatFileSize(this, cacheSize)
        )
    }

    companion object {
        const val TAG: String = "OSM"

        /**
         * refreshes the current osmdroid cache paths with user preferences plus soe logic to work around
         * file system permissions on api23 devices. it's primarily used for out android tests.
         *
         * @param ctx
         * @return current cache size in bytes
         */
        @JvmStatic
        fun updateStoragePreferences(ctx: Context): Long {
            //loads the osmdroid config from the shared preferences object.
            //if this is the first time launching this app, all settings are set defaults with one exception,
            //the tile cache. the default is the largest write storage partition, which could end up being
            //this app's private storage, depending on device config and permissions

            instance!!.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))


            //also note that our preference activity has the corresponding save method on the config object, but it can be called at any time.
            val dbFile = File(instance!!.osmdroidTileCache!!.getAbsolutePath() + File.separator + SqlTileWriter.DATABASE_FILENAME)
            if (dbFile.exists()) {
                return dbFile.length()
            }
            return -1
        }
    }
}

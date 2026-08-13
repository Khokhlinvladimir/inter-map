// Created by plusminus on 18:23:13 - 03.10.2008
package org.osmdroid

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.osmdroid.config.Configuration.instance
import org.osmdroid.debug.CacheAnalyzerActivity
import org.osmdroid.diag.DiagnosticsActivity
import org.osmdroid.intro.IntroActivity
import org.osmdroid.samples.SampleWithMinimapItemizedoverlay
import org.osmdroid.samples.SampleWithTilesOverlay
import org.osmdroid.samples.SampleWithTilesOverlayAndCustomTileSource
import org.osmdroid.tileprovider.modules.SqlTileWriter
import java.io.File

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private lateinit var destinations: List<DemoDestination>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        destinations = createDestinations()
        findViewById<ListView>(R.id.activitylist).apply {
            adapter = DestinationAdapter(this@MainActivity, destinations)
            onItemClickListener = this@MainActivity
        }
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun createDestinations() = listOf(
        DemoDestination(R.string.menu_starter_title, R.string.menu_starter_subtitle, R.drawable.ic_material_map, Destination.STARTER),
        DemoDestination(R.string.menu_itemized_title, R.string.menu_itemized_subtitle, R.drawable.ic_material_place, Destination.ITEMIZED),
        DemoDestination(R.string.menu_tiles_title, R.string.menu_tiles_subtitle, R.drawable.ic_material_layers, Destination.TILE_OVERLAY),
        DemoDestination(R.string.menu_custom_tiles_title, R.string.menu_custom_tiles_subtitle, R.drawable.ic_material_tiles, Destination.CUSTOM_TILES),
        DemoDestination(R.string.menu_samples_title, R.string.menu_samples_subtitle, R.drawable.ic_material_explore, Destination.ALL_SAMPLES),
        DemoDestination(R.string.menu_report_title, R.string.menu_report_subtitle, R.drawable.ic_material_bug_report, Destination.REPORT),
        DemoDestination(R.string.menu_settings_title, R.string.menu_settings_subtitle, R.drawable.ic_material_settings, Destination.SETTINGS),
        DemoDestination(R.string.menu_bugs_title, R.string.menu_bugs_subtitle, R.drawable.ic_material_science, Destination.BUG_DRIVERS),
        DemoDestination(R.string.menu_diagnostics_title, R.string.menu_diagnostics_subtitle, R.drawable.ic_material_monitoring, Destination.DIAGNOSTICS),
        DemoDestination(R.string.menu_intro_title, R.string.menu_intro_subtitle, R.drawable.ic_material_info, Destination.INTRO),
        DemoDestination(R.string.menu_licenses_title, R.string.menu_licenses_subtitle, R.drawable.ic_material_description, Destination.LICENSES),
        DemoDestination(R.string.menu_cache_title, R.string.menu_cache_subtitle, R.drawable.ic_material_storage, Destination.CACHE_ANALYZER),
    )

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (destinations[position].destination) {
            Destination.STARTER -> startActivity(Intent(this, StarterMapActivity::class.java))
            Destination.ITEMIZED -> startActivity(Intent(this, SampleWithMinimapItemizedoverlay::class.java))
            Destination.TILE_OVERLAY -> startActivity(Intent(this, SampleWithTilesOverlay::class.java))
            Destination.CUSTOM_TILES -> startActivity(Intent(this, SampleWithTilesOverlayAndCustomTileSource::class.java))
            Destination.ALL_SAMPLES -> startActivity(Intent(this, ExtraSamplesActivity::class.java))
            Destination.REPORT -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FORK_ISSUES_URL)))
            Destination.SETTINGS -> startActivity(Intent(this, PreferenceActivity::class.java))
            Destination.BUG_DRIVERS -> startActivity(Intent(this, BugsTestingActivity::class.java))
            Destination.DIAGNOSTICS -> startActivity(Intent(this, DiagnosticsActivity::class.java))
            Destination.INTRO -> {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .remove("osmdroid_first_ran")
                    .apply()
                startActivity(Intent(this, IntroActivity::class.java))
                finish()
            }
            Destination.LICENSES -> startActivity(Intent(this, LicenseActivity::class.java))
            Destination.CACHE_ANALYZER -> startActivity(Intent(this, CacheAnalyzerActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStorageInfo()
        checkForCrashLogs()
    }

    private fun checkForCrashLogs() {
        val file = File(Environment.getExternalStorageDirectory(), "/osmdroid/crash.log")
        if (!file.exists() || !file.canRead()) return

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("osmdroidbugs@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Open Map crash log")
                        putExtra(Intent.EXTRA_TEXT, "Log data")
                        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                    }
                    startActivity(Intent.createChooser(emailIntent, "Pick an email provider"))
                }
                DialogInterface.BUTTON_NEGATIVE -> file.delete()
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Crash logs")
            .setMessage("The demo previously stopped unexpectedly. Would you like to share its crash log?")
            .setPositiveButton("Share", dialogClickListener)
            .setNegativeButton("Delete", dialogClickListener)
            .show()
    }

    private fun updateStorageInfo() {
        val cacheSize = updateStoragePreferences(this)
        val storageAvailable = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        findViewById<TextView>(R.id.sdcardstate_value).apply {
            setText(if (storageAvailable) R.string.home_storage_available else R.string.home_storage_unavailable)
            isEnabled = storageAvailable
        }

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            ?.removeSuffix("-SNAPSHOT")
            .orEmpty()
        findViewById<TextView>(R.id.version_text).text = "$versionName ${BuildConfig.BUILD_TYPE}"

        val cachePath = instance?.osmdroidTileCache?.absolutePath.orEmpty()
        val formattedSize = if (cacheSize >= 0) Formatter.formatFileSize(this, cacheSize) else "—"
        findViewById<TextView>(R.id.mainstorageInfo).text = "$cachePath\n${getString(R.string.home_cache_label)} · $formattedSize"
    }

    private data class DemoDestination(
        @StringRes val title: Int,
        @StringRes val subtitle: Int,
        @DrawableRes val icon: Int,
        val destination: Destination,
    )

    private enum class Destination {
        STARTER, ITEMIZED, TILE_OVERLAY, CUSTOM_TILES, ALL_SAMPLES, REPORT,
        SETTINGS, BUG_DRIVERS, DIAGNOSTICS, INTRO, LICENSES, CACHE_ANALYZER,
    }

    private class DestinationAdapter(
        private val context: Context,
        private val items: List<DemoDestination>,
    ) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].destination.ordinal.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_main_destination, parent, false)
            val item = getItem(position)
            view.findViewById<ImageView>(R.id.destination_icon).setImageResource(item.icon)
            view.findViewById<TextView>(R.id.destination_title).setText(item.title)
            view.findViewById<TextView>(R.id.destination_subtitle).setText(item.subtitle)
            return view
        }
    }

    companion object {
        const val TAG = "OSM"
        private const val FORK_ISSUES_URL = "https://github.com/Khokhlinvladimir/inter-map/issues/new"

        @JvmStatic
        fun updateStoragePreferences(ctx: Context): Long {
            instance?.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
            val cache = instance?.osmdroidTileCache ?: return -1
            val dbFile = File(cache, SqlTileWriter.DATABASE_FILENAME)
            return if (dbFile.exists()) dbFile.length() else -1
        }
    }
}

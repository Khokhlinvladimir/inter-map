package org.osmdroid.debug

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.R
import org.osmdroid.debug.browser.CacheBrowserActivity
import org.osmdroid.debug.model.SqlTileWriterExt
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.util.Counters

/**
 * A debug utility to show various cache metrics and management
 *
 *
 * requires api11+ due to the use of sqlite
 *
 *
 * created on 12/21/2016.
 *
 * @author Alex O'Ree
 * @since 5.6.2
 */
class CacheAnalyzerActivity : AppCompatActivity(), OnItemClickListener, Runnable {
    var cache: SqlTileWriterExt? = null
    var cacheStats: TextView? = null
    var show: AlertDialog? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cache_analyzer)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        cacheStats = findViewById<TextView?>(R.id.cacheStats)

        val list = ArrayList<String?>()
        list.add("Browse the cache")
        list.add("Purge the cache")
        list.add("Purge a specific tile source")
        list.add("See the debug counters")

        val lv = findViewById<ListView>(R.id.statslist)
        val adapter: ArrayAdapter<*> = ArrayAdapter<String?>(this, android.R.layout.simple_list_item_1, list)

        lv.setAdapter(adapter)
        lv.setOnItemClickListener(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    public override fun onResume() {
        super.onResume()
        cache = SqlTileWriterExt()
        Thread(this).start()
    }

    public override fun onPause() {
        super.onPause()
        cache!!.onDetach()
        cache = null
        if (show != null) show!!.dismiss()
        show = null
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (position) {
            0 -> this.startActivity(Intent(this, CacheBrowserActivity::class.java))
            1 -> purgeCache()
            2 -> purgeTileSource()
            3 -> showDebugCounters()
        }
    }

    private fun showDebugCounters() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tile Source")
        val sb = StringBuilder()
        sb.append(Counters::class.java.getCanonicalName() + "\nPerformance and debug counters\n\n")
        sb.append("Out of memory errors: " + Counters.countOOM + "\n")
        sb.append("File cache hit: " + Counters.fileCacheHit + "\n")
        sb.append("File cache miss: " + Counters.fileCacheMiss + "\n")
        sb.append("File cache oom: " + Counters.fileCacheOOM + "\n")
        sb.append("File cache save errors: " + Counters.fileCacheSaveErrors + "\n")
        sb.append("Tile download errors: " + Counters.tileDownloadErrors + "\n")
        builder.setMessage(sb.toString())

        show = builder.show()
    }

    private fun purgeTileSource() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tile Source")

        val arrayAdapter = ArrayAdapter<String?>(this, android.R.layout.select_dialog_singlechoice)
        val sources = cache!!.sources
        for (i in sources.indices) {
            arrayAdapter.add(sources[i]!!.source)
        }

        builder.setAdapter(arrayAdapter, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val item = arrayAdapter.getItem(which)
                val b = cache!!.purgeCache(item)
                if (b) Toast.makeText(this@CacheAnalyzerActivity, "SQL Cache purged", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this@CacheAnalyzerActivity, "SQL Cache purge failed, see logcat for details", Toast.LENGTH_LONG).show()
            }
        })
        builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
            }
        })

        builder.show()
    }

    private fun purgeCache() {
        var sqlTileWriter: SqlTileWriter? = SqlTileWriter()
        val b = sqlTileWriter!!.purgeCache()
        sqlTileWriter.onDetach()
        sqlTileWriter = null
        if (b) Toast.makeText(this, "SQL Cache purged", Toast.LENGTH_SHORT).show()
        else Toast.makeText(this, "SQL Cache purge failed, see logcat for details", Toast.LENGTH_LONG).show()
    }

    override fun run() {
        if (cache == null) return
        val sources = cache!!.sources
        val sb = StringBuilder("Source: tile count\n")
        if (sources.isEmpty()) sb.append("None")
        for (sourceCount in sources) {
            sourceCount!!
            sb.append("Source ").append(sourceCount.source)
            sb.append(": count=").append(sourceCount.rowCount)
            sb.append("; minsize=").append(sourceCount.sizeMin)
            sb.append("; maxsize=").append(sourceCount.sizeMax)
            sb.append("; totalsize=").append(sourceCount.sizeTotal)
            sb.append("; avgsize=").append(sourceCount.sizeAvg)
            sb.append("\n")
        }
        var expired: Long = 0
        if (cache != null) expired = cache!!.rowCountExpired
        sb.append("Expired tiles: " + expired)

        this.runOnUiThread(object : Runnable {
            override fun run() {
                try {
                    val tv = findViewById<TextView?>(R.id.cacheStats)

                    if (tv != null) {
                        tv.setText(sb.toString())
                    }
                } catch (ex: Exception) {
                }
            }
        })
    }
}

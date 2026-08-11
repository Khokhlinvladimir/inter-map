package org.osmdroid.debug.browser

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.MainActivity
import org.osmdroid.R
import org.osmdroid.debug.model.SqlTileWriterExt
import org.osmdroid.debug.util.FileDateUtil
import org.osmdroid.intro.StorageAdapter

/**
 * A simple view for browsing the osmdroid tile cache database
 * created on 12/20/2016.
 *
 * @author Alex O'Ree
 * @see org.osmdroid.debug.CacheAnalyzerActivity
 *
 * @since 5.6.2
 */
class CacheBrowserActivity : AppCompatActivity() {
    var cache: SqlTileWriterExt? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cache_browser)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    public override fun onResume() {
        super.onResume()

        cache = SqlTileWriterExt()
        val adapter = CacheAdapter(this, cache!!)

        val lv = findViewById<ListView>(R.id.cacheListView)
        lv.setAdapter(adapter)

        (findViewById<View?>(R.id.rows) as TextView).setText(cache!!.getRowCount(null).toString() + "")
        (findViewById<View?>(R.id.size) as TextView).setText(StorageAdapter.readableFileSize(MainActivity.updateStoragePreferences(this)))
        (findViewById<View?>(R.id.date) as TextView).setText("Now " + FileDateUtil.getModifiedDate(System.currentTimeMillis()))
    }

    public override fun onPause() {
        super.onPause()
        cache!!.onDetach()
        cache = null
    }
}

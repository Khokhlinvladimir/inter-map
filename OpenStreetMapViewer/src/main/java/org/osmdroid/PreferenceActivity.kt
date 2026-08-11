package org.osmdroid

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.osmdroid.config.Configuration.instance
import org.osmdroid.config.Configuration.setConfigurationProvider
import org.osmdroid.config.DefaultConfigurationProvider
import org.osmdroid.intro.StorageAdapter
import org.osmdroid.model.PositiveLongTextValidator
import org.osmdroid.model.PositiveShortTextValidator
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.util.StorageUtils
import java.io.File

/**
 * OK so why is here?
 * Stupid reason #1: Android Studio's wizard generates a bunch of stupid complex code
 * Stupid reason #2: Android's Preference Activity is API10+ and we (osmdroid) are API8+
 * Stupid reason #3: Simple is better, usually
 *
 * @since 5.6
 * Created by alex on 10/21/16.
 */
class PreferenceActivity : AppCompatActivity(), View.OnClickListener {
    var checkBoxDebugTileProvider: CheckBox? = null
    var checkBoxDebugMode: CheckBox? = null
    var checkBoxHardwareAcceleration: CheckBox? = null
    var checkBoxMapViewDebug: CheckBox? = null
    var checkBoxDebugDownloading: CheckBox? = null
    var buttonSetCache: Button? = null
    var buttonManualCacheEntry: Button? = null
    var buttonPurgeCache: Button? = null
    var buttonReset: Button? = null
    var buttonSetBase: Button? = null
    var buttonManualBaseEntry: Button? = null
    var textViewCacheDirectory: TextView? = null
    var textViewBaseDirectory: TextView? = null
    var httpUserAgent: EditText? = null
    var tileDownloadThreads: EditText? = null
    var tileDownloadMaxQueueSize: EditText? = null
    var cacheMapTileCount: EditText? = null
    var cacheMaxSize: EditText? = null
    var cacheTrimSize: EditText? = null
    var tileFileSystemThreads: EditText? = null
    var tileFileSystemMaxQueueSize: EditText? = null
    var gpsWaitTime: EditText? = null
    var additionalExpirationTime: EditText? = null
    var overrideExpirationTime: EditText? = null
    var zoomSpeedDefault: EditText? = null
    var zoomSpeedShort: EditText? = null
    var abortSave: Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prefs)
        val myToolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        setSupportActionBar(myToolbar)

        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)

        checkBoxDebugTileProvider = findViewById<CheckBox>(R.id.checkBoxDebugTileProvider)
        checkBoxDebugMode = findViewById<CheckBox>(R.id.checkBoxDebugMode)
        checkBoxHardwareAcceleration = findViewById<CheckBox>(R.id.checkBoxHardwareAcceleration)
        checkBoxDebugDownloading = findViewById<CheckBox>(R.id.checkBoxDebugDownloading)
        checkBoxMapViewDebug = findViewById<CheckBox>(R.id.checkBoxMapViewDebug)
        checkBoxDebugTileProvider!!.setOnClickListener(this)
        checkBoxDebugMode!!.setOnClickListener(this)
        checkBoxHardwareAcceleration!!.setOnClickListener(this)
        checkBoxMapViewDebug!!.setOnClickListener(this)

        textViewCacheDirectory = findViewById<TextView>(R.id.textViewCacheDirectory)
        textViewBaseDirectory = findViewById<TextView>(R.id.textViewBaseDirectory)
        buttonPurgeCache = findViewById<Button>(R.id.buttonPurgeCache)
        httpUserAgent = findViewById<EditText>(R.id.httpUserAgent)
        tileDownloadThreads = findViewById<EditText>(R.id.tileDownloadThreads)
        tileDownloadThreads!!.addTextChangedListener(PositiveShortTextValidator(tileDownloadThreads!!))
        tileDownloadMaxQueueSize = findViewById<EditText>(R.id.tileDownloadMaxQueueSize)
        tileDownloadMaxQueueSize!!.addTextChangedListener(PositiveShortTextValidator(tileDownloadMaxQueueSize!!))
        cacheMapTileCount = findViewById<EditText>(R.id.cacheMapTileCount)
        cacheMapTileCount!!.addTextChangedListener(PositiveShortTextValidator(cacheMapTileCount!!))
        tileFileSystemThreads = findViewById<EditText>(R.id.tileFileSystemThreads)
        tileFileSystemThreads!!.addTextChangedListener(PositiveShortTextValidator(tileFileSystemThreads!!))
        tileFileSystemMaxQueueSize = findViewById<EditText>(R.id.tileFileSystemMaxQueueSize)
        tileFileSystemMaxQueueSize!!.addTextChangedListener(PositiveShortTextValidator(tileFileSystemMaxQueueSize!!))
        gpsWaitTime = findViewById<EditText>(R.id.gpsWaitTime)
        gpsWaitTime!!.addTextChangedListener(PositiveLongTextValidator(gpsWaitTime!!, 1))
        additionalExpirationTime = findViewById<EditText>(R.id.additionalExpirationTime)
        additionalExpirationTime!!.addTextChangedListener(PositiveLongTextValidator(additionalExpirationTime!!, 0))

        cacheMaxSize = findViewById<EditText>(R.id.cacheMaxSize)
        cacheTrimSize = findViewById<EditText>(R.id.cacheTrimSize)
        cacheMaxSize!!.addTextChangedListener(PositiveLongTextValidator(cacheMaxSize!!, 0))
        cacheTrimSize!!.addTextChangedListener(PositiveLongTextValidator(cacheTrimSize!!, 0))

        overrideExpirationTime = findViewById<EditText>(R.id.overrideExpirationTime)
        zoomSpeedDefault = findViewById<EditText>(R.id.zoomSpeedDefault)
        zoomSpeedDefault!!.addTextChangedListener(PositiveLongTextValidator(zoomSpeedDefault!!, 1))
        zoomSpeedShort = findViewById<EditText>(R.id.zoomSpeedShort)
        zoomSpeedShort!!.addTextChangedListener(PositiveLongTextValidator(zoomSpeedShort!!, 1))


        buttonSetBase = findViewById<Button>(R.id.buttonSetBase)
        buttonSetBase!!.setOnClickListener(this)
        buttonSetCache = findViewById<Button>(R.id.buttonSetCache)
        buttonManualCacheEntry = findViewById<Button>(R.id.buttonManualCacheEntry)
        buttonSetCache!!.setOnClickListener(this)
        buttonManualBaseEntry = findViewById<Button>(R.id.buttonManualBaseEntry)
        buttonManualBaseEntry!!.setOnClickListener(this)
        buttonManualCacheEntry!!.setOnClickListener(this)
        buttonPurgeCache!!.setOnClickListener(this)
        buttonReset = findViewById<Button>(R.id.buttonReset)
        buttonReset!!.setOnClickListener(this)

        findViewById<View?>(R.id.baseDirTitle).setOnClickListener(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    public override fun onResume() {
        super.onResume()
        tileFileSystemMaxQueueSize!!.setText(instance!!.tileFileSystemMaxQueueSize.toString() + "")
        tileFileSystemThreads!!.setText(instance!!.tileFileSystemThreads.toString() + "")
        tileDownloadMaxQueueSize!!.setText(instance!!.tileDownloadMaxQueueSize.toString() + "")
        tileDownloadThreads!!.setText(instance!!.tileDownloadThreads.toString() + "")
        gpsWaitTime!!.setText(instance!!.gpsWaitTime.toString() + "")
        additionalExpirationTime!!.setText(instance!!.expirationExtendedDuration.toString() + "")
        cacheMapTileCount!!.setText(instance!!.cacheMapTileCount.toString() + "")
        if (instance!!.expirationOverrideDuration != null) overrideExpirationTime!!.setText(instance!!.expirationOverrideDuration.toString() + "")

        httpUserAgent!!.setText(instance!!.userAgentValue)
        checkBoxMapViewDebug!!.setChecked(instance!!.isDebugMapView)
        checkBoxDebugMode!!.setChecked(instance!!.isDebugMode)
        checkBoxDebugTileProvider!!.setChecked(instance!!.isDebugTileProviders)
        checkBoxHardwareAcceleration!!.setChecked(instance!!.isMapViewHardwareAccelerated)
        checkBoxDebugDownloading!!.setChecked(instance!!.isDebugMapTileDownloader)
        textViewCacheDirectory!!.setText(instance!!.osmdroidTileCache!!.getAbsolutePath())
        textViewBaseDirectory!!.setText(instance!!.osmdroidBasePath!!.getAbsolutePath())

        cacheMaxSize!!.setText(instance!!.tileFileSystemCacheMaxBytes.toString() + "")
        cacheTrimSize!!.setText(instance!!.tileFileSystemCacheTrimBytes.toString() + "")

        zoomSpeedDefault!!.setText(instance!!.animationSpeedDefault.toString() + "")
        zoomSpeedShort!!.setText(instance!!.animationSpeedShort.toString() + "")
    }

    public override fun onPause() {
        super.onPause()
        if (abortSave) return
        //save the configuration
        try {
            if (tileDownloadThreads!!.getError() == null) instance!!.tileDownloadThreads = tileDownloadThreads!!.getText().toString().toShort()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            if (tileDownloadMaxQueueSize!!.getError() == null) instance!!.tileDownloadMaxQueueSize =
                tileDownloadMaxQueueSize!!.getText().toString().toShort()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            if (cacheMapTileCount!!.getError() == null) instance!!.cacheMapTileCount = cacheMapTileCount!!.getText().toString().toShort()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            if (tileFileSystemThreads!!.getError() == null) instance!!.tileFileSystemThreads = tileFileSystemThreads!!.getText().toString().toShort()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            if (tileFileSystemMaxQueueSize!!.getError() == null) instance!!.tileFileSystemMaxQueueSize =
                tileFileSystemMaxQueueSize!!.getText().toString().toShort()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            if (gpsWaitTime!!.getError() == null) instance!!.gpsWaitTime = gpsWaitTime!!.getText().toString().toLong()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            if (additionalExpirationTime!!.getError() == null) instance!!.expirationExtendedDuration =
                additionalExpirationTime!!.getText().toString().toLong()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            val `val` = overrideExpirationTime!!.getText().toString().toLong()
            if (`val` > 0) instance!!.expirationOverrideDuration = `val`
            else instance!!.expirationOverrideDuration = null
        } catch (ex: Exception) {
            ex.printStackTrace()
            instance!!.expirationOverrideDuration = null
        }

        try {
            val `val` = cacheMaxSize!!.getText().toString().toLong()
            if (`val` > 0) instance!!.tileFileSystemCacheMaxBytes = `val`
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        try {
            val `val` = cacheTrimSize!!.getText().toString().toLong()
            if (`val` > 0) instance!!.tileFileSystemCacheTrimBytes = `val`
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        instance!!.userAgentValue = httpUserAgent!!.getText().toString()
        instance!!.isDebugMapView = checkBoxMapViewDebug!!.isChecked()
        instance!!.isDebugMode = checkBoxDebugMode!!.isChecked()
        instance!!.isDebugTileProviders = checkBoxDebugTileProvider!!.isChecked()
        instance!!.isMapViewHardwareAccelerated = checkBoxHardwareAcceleration!!.isChecked()
        instance!!.isDebugMapTileDownloader = checkBoxDebugDownloading!!.isChecked()
        instance!!.osmdroidTileCache = File(textViewCacheDirectory!!.getText().toString())
        instance!!.osmdroidBasePath = File(textViewBaseDirectory!!.getText().toString())

        try {
            val `val` = zoomSpeedDefault!!.getText().toString().toInt()
            if (`val` > 0) instance!!.animationSpeedDefault = `val`
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        try {
            val `val` = zoomSpeedShort!!.getText().toString().toInt()
            if (`val` > 0) instance!!.animationSpeedShort = `val`
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        instance!!.save(this, prefs)
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.buttonManualCacheEntry -> {
                showManualEntry(textViewCacheDirectory!!)
            }

            R.id.buttonSetCache -> {
                showPickCacheFromList(textViewCacheDirectory!!, "tiles" + File.separator)
            }

            R.id.buttonPurgeCache -> {
                purgeCache()
            }

            R.id.buttonReset -> {
                resetSettings(this)
                abortSave = true
                finish()
            }

            R.id.buttonManualBaseEntry -> {
                showManualEntry(textViewBaseDirectory!!)
            }

            R.id.buttonSetBase -> {
                showPickCacheFromList(textViewBaseDirectory!!, "")
            }
        }
    }

    private fun purgeCache() {
        val dialogClickListener: DialogInterface.OnClickListener = object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                when (which) {
                    DialogInterface.BUTTON_POSITIVE ->                         //Yes button clicked
                        Thread(object : Runnable {
                            override fun run() {
                                val sqlTileWriter = SqlTileWriter()
                                val b = sqlTileWriter.purgeCache()
                                sqlTileWriter.onDetach()
                                val title = if (b) "SQL Cache purged" else "SQL Cache purge failed, see logcat for details"
                                val length = if (b) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                this@PreferenceActivity.runOnUiThread(object : Runnable {
                                    override fun run() {
                                        Toast.makeText(this@PreferenceActivity, title, length).show()
                                    }
                                })
                            }
                        }).start()

                    DialogInterface.BUTTON_NEGATIVE -> {}
                }
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.userconfirm).setPositiveButton(R.string.yes, dialogClickListener)
            .setNegativeButton(R.string.no, dialogClickListener).show()
    }

    private fun showPickCacheFromList(tv: TextView, postfix: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.enterCacheLocation)

        val storageList = StorageUtils.getStorageList(this)
        val storageListFiltered: MutableList<StorageUtils.StorageInfo?> = ArrayList<StorageUtils.StorageInfo?>()
        for (storageInfo in storageList) {
            if (!storageInfo.readonly) {
                storageListFiltered.add(storageInfo)
            }
        }

        val arrayAdapter = StorageAdapter(this, storageListFiltered)

        builder.setAdapter(arrayAdapter, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val item = arrayAdapter.getItem(which) as StorageUtils.StorageInfo?
                try {
                    File(item!!.path + File.separator + "osmdroid" + File.separator + postfix).mkdirs()
                    tv.setText(item.path + File.separator + "osmdroid" + File.separator + postfix)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Toast.makeText(this@PreferenceActivity, "Invalid entry: " + ex.message, Toast.LENGTH_LONG).show()
                }
            }
        })
        builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
            }
        })

        builder.show()
    }


    private fun showManualEntry(textView: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.enterCacheLocation)

        // Set up the input
        val input = EditText(this)
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        input.setLines(1)
        input.setText(textView.getText().toString())
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val file = File(input.getText().toString())
                if (!file.exists()) {
                    input.setError("Does not exist")
                } else if (file.exists() && !file.isDirectory()) {
                    input.setError("Not a directory")
                } else if (!StorageUtils.isWritable(file)) {
                    input.setError("Not writable")
                } else {
                    input.setError(null)
                }
            }
        })
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                if (input.getError() == null) {
                    textView.setText(input.getText().toString())
                }
            }
        })
        builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
            }
        })

        builder.show()
    }


    companion object {
        fun resetSettings(ctx: Context?) {
            //delete all preference keys, if you're using this for your own application
            //you may want to consider some additional logic here (only clear osmdroid settings or
            //use something other than the default shared preferences map
            val edit = PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            edit.clear()
            edit.commit()
            //this will repopulate the default settings
            setConfigurationProvider(DefaultConfigurationProvider())
            //this will save the default along with the user agent (important for downloading tiles)
            instance!!.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        }
    }
}
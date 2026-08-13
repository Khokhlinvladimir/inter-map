package org.osmdroid.intro

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.osmdroid.R
import org.osmdroid.config.Configuration.instance
import org.osmdroid.intro.StorageAdapter.Companion.readableFileSize
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.util.StorageUtils
import java.io.File

/**
 * created on 1/5/2017.
 *
 * @author Alex O'Ree
 */
class StoragePreferenceFragment : Fragment(), View.OnClickListener {
    var buttonSetCache: Button? = null
    var buttonManualCacheEntry: Button? = null
    var textViewCacheDirectory: TextView? = null
    var textViewCacheMaxSize: TextView? = null
    var textViewCacheFreeSpace: TextView? = null
    var textViewCacheCurrentSize: TextView? = null
    var textViewCacheTrimSize: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.intro_storage, container, false)

        textViewCacheDirectory = v.findViewById<TextView>(R.id.textViewCacheDirectory)
        buttonSetCache = v.findViewById<Button>(R.id.buttonSetCache)
        buttonManualCacheEntry = v.findViewById<Button>(R.id.buttonManualCacheEntry)
        buttonSetCache!!.setOnClickListener(this)
        buttonManualCacheEntry!!.setOnClickListener(this)
        textViewCacheMaxSize = v.findViewById<TextView>(R.id.textViewCacheMaxSize)
        textViewCacheFreeSpace = v.findViewById<TextView>(R.id.textViewCacheFreeSpace)
        textViewCacheCurrentSize = v.findViewById<TextView>(R.id.textViewCacheCurrentSize)
        textViewCacheTrimSize = v.findViewById<TextView>(R.id.textViewCacheTrimSize)
        return v
    }

    override fun onResume() {
        super.onResume()
        updateStorage(requireContext())

        textViewCacheDirectory!!.setText(instance!!.osmdroidTileCache.toString())
        textViewCacheMaxSize!!.setText(readableFileSize(instance!!.tileFileSystemCacheMaxBytes))
        textViewCacheTrimSize!!.setText(readableFileSize(instance!!.tileFileSystemCacheTrimBytes))
        textViewCacheFreeSpace!!.setText(readableFileSize(instance!!.osmdroidTileCache!!.getFreeSpace()))

        val dbFile = File(instance!!.osmdroidTileCache!!.getAbsolutePath() + File.separator + SqlTileWriter.DATABASE_FILENAME)
        if (dbFile.exists()) {
            textViewCacheCurrentSize!!.setText(readableFileSize(dbFile.length()))
        } else {
            textViewCacheCurrentSize!!.setText("")
        }
    }

    fun updateStorage(ctx: Context) {
        //only needed for api23+ since we "should" have had permissions granted by now
        instance!!.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.buttonManualCacheEntry -> showManualEntry()
            R.id.buttonSetCache -> showPickCacheFromList()
        }
    }

    private fun showPickCacheFromList() {
        val builder = AlertDialog.Builder(this.getContext())
        builder.setTitle(R.string.enterCacheLocation)

        val storageList = StorageUtils.getStorageList(getActivity())
        val storageListFiltered: MutableList<StorageUtils.StorageInfo?> = ArrayList<StorageUtils.StorageInfo?>()
        for (storageInfo in storageList) {
            if (!storageInfo.readonly) {
                storageListFiltered.add(storageInfo)
            }
        }

        val arrayAdapter = StorageAdapter(this.getContext()!!, storageListFiltered)

        builder.setAdapter(arrayAdapter, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val item = arrayAdapter.getItem(which) as StorageUtils.StorageInfo?
                try {
                    File(item!!.path + File.separator + "osmdroid" + File.separator + "tiles" + File.separator).mkdirs()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                textViewCacheDirectory!!.setText(item!!.path + File.separator + "osmdroid" + File.separator + "tiles")
                instance!!.osmdroidTileCache = File(textViewCacheDirectory!!.getText().toString() + "")
                instance!!.save(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()))

                textViewCacheMaxSize!!.setText(readableFileSize(instance!!.tileFileSystemCacheMaxBytes))
                textViewCacheTrimSize!!.setText(readableFileSize(instance!!.tileFileSystemCacheTrimBytes))

                textViewCacheFreeSpace!!.setText(readableFileSize(instance!!.osmdroidTileCache!!.getFreeSpace()))
                val dbFile = File(instance!!.osmdroidTileCache!!.getAbsolutePath() + File.separator + SqlTileWriter.DATABASE_FILENAME)
                if (dbFile.exists()) {
                    textViewCacheCurrentSize!!.setText(readableFileSize(dbFile.length()))
                } else {
                    textViewCacheCurrentSize!!.setText("")
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


    private fun showManualEntry() {
        val builder = AlertDialog.Builder(this.getContext())
        builder.setTitle(R.string.enterCacheLocation)

        // Set up the input
        val input = EditText(this.getContext())
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        input.setLines(1)
        input.setText(textViewCacheDirectory!!.getText().toString())
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
                    textViewCacheDirectory!!.setText(input.getText().toString())

                    textViewCacheMaxSize!!.setText(readableFileSize(instance!!.tileFileSystemCacheMaxBytes))
                    textViewCacheTrimSize!!.setText(readableFileSize(instance!!.tileFileSystemCacheTrimBytes))

                    textViewCacheFreeSpace!!.setText(readableFileSize(instance!!.osmdroidTileCache!!.getFreeSpace()))
                    val dbFile = File(instance!!.osmdroidTileCache!!.getAbsolutePath() + File.separator + SqlTileWriter.DATABASE_FILENAME)
                    if (dbFile.exists()) {
                        textViewCacheCurrentSize!!.setText(readableFileSize(dbFile.length()))
                    } else {
                        textViewCacheCurrentSize!!.setText("")
                    }
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
}

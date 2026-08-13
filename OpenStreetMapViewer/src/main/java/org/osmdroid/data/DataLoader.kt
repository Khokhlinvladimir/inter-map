package org.osmdroid.data

import android.content.Context
import androidx.annotation.RawRes
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.Reader

/**
 * [DataRegion] json loader
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
abstract class DataLoader<T>(pContext: Context, @RawRes pResId: Int) {
    val list: LinkedHashMap<String?, T?> = LinkedHashMap<String?, T?>()

    init {
        load(getJsonString(pContext, pResId))
    }

    @Throws(JSONException::class)
    protected abstract fun getItem(pKey: String?, pJsonObject: JSONObject?): T?

    @Throws(Exception::class)
    private fun load(pJson: String) {
        val root = JSONObject(pJson)
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val region = root.getJSONObject(key)
            list.put(key, getItem(key, region))
        }
    }

    @Throws(Exception::class)
    private fun getJsonString(pContext: Context, @RawRes pResource: Int): String {
        val `is` = pContext.getResources().openRawResource(pResource)
        val bis = BufferedInputStream(`is`)
        val bufferSize = 1024 * 64
        val buffer = CharArray(bufferSize)
        val out = StringBuilder()
        val `in`: Reader = InputStreamReader(bis, "UTF-8")
        var read: Int
        while ((`in`.read(buffer, 0, buffer.size).also { read = it }) > 0) {
            out.append(buffer, 0, read)
        }
        `is`.close()
        return out.toString()
    }
}
